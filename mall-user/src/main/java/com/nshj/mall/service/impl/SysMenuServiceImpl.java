package com.nshj.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nshj.mall.constant.RedisConstants;
import com.nshj.mall.constant.RocketMqConstants;
import com.nshj.mall.entity.SysMenu;
import com.nshj.mall.exception.BusinessException;
import com.nshj.mall.mapper.SysMenuMapper;
import com.nshj.mall.service.SysMenuService;
import com.nshj.mall.utils.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单业务逻辑核心实现 (Core Menu Implementation)
 * <p>
 * <b>技术实现架构：</b>
 * <ol>
 * <li><b>内存计算 (In-Memory Processing):</b> 树形结构的组装完全在 JVM 内存中通过递归完成。相比于数据库递归查询，一次全量 Fetch + 内存组装能显著降低数据库 I/O 压力。</li>
 * <li><b>防御性编程 (Defensive Programming):</b> 删除操作前执行结构约束检查 (Fail-Fast)，防止树形结构断裂。</li>
 * <li><b>分布式一致性 (Event-Driven Consistency):</b> 关键写操作 (CUD) 成功后，自动触发 "缓存失效" 与 "MQ广播"，确保集群中所有节点的权限缓存同步刷新。</li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final RedisCache redisCache;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public List<SysMenu> getMenuList(String menuName) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        // 组合查询条件：模糊匹配 + 双重排序 (保证列表展示的稳定性)
        wrapper.like(StringUtils.hasText(menuName), SysMenu::getMenuName, menuName)
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMenu(SysMenu sysMenu) {
        // 1. 执行数据库持久化
        boolean success = this.save(sysMenu);

        // 2. 事务提交前触发副作用 (缓存清理 + 消息广播)
        if (success) {
            dispatchMenuChangeEvent("创建新菜单: " + sysMenu.getMenuName());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(SysMenu sysMenu) {
        // 1. 执行数据库更新
        boolean success = this.updateById(sysMenu);

        // 2. 事务提交前触发副作用
        if (success) {
            dispatchMenuChangeEvent("更新菜单 ID: " + sysMenu.getId());
        }
    }

    @Override
    public List<String> getPermsByUserId(Long userId) {
        // 委托 Mapper 执行多表 Join 查询 (sys_user -> sys_user_role -> sys_role_menu -> sys_menu)
        // 确保仅返回 status=1 (正常) 的权限标识
        return sysMenuMapper.selectPermsByUserId(userId);
    }

    @Override
    public List<SysMenu> getTreeMenu() {
        // Step 1: 获取全量数据 (通常系统菜单数量级在 100~500 之间，一次全查性能最优，避免 N+1 问题)
        List<SysMenu> allMenus = this.list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder));

        // Step 2: 执行内存递归组装
        return buildTree(allMenus);
    }

    @Override
    public List<SysMenu> getMenuRules() {
        // 仅查询类型为 "接口/API" 且需要鉴权的规则
        return sysMenuMapper.selectMenuRules();
    }

    /**
     * 批量删除菜单资源
     * <p>
     * <b>性能优化：</b>
     * 使用 {@code IN} 查询一次性校验所有待删除节点的子节点状态，将数据库交互复杂度从 O(N) 降为 O(1)。
     *
     * @param menuIds 目标菜单 ID 集合
     * @throws BusinessException 当检测到任一菜单下仍存在子菜单时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMenuBatchByIds(List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) return;

        // 1. 约束校验 (Check Constraint)
        // 检查是否存在 ParentId 在待删除列表中的记录
        long childCount = this.count(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getParentId, menuIds));

        if (childCount > 0) {
            throw new BusinessException("操作拒绝：所选菜单中包含存在子节点的菜单，请先清理子节点");
        }

        // 2. 执行批量物理删除
        boolean success = this.removeBatchByIds(menuIds);

        // 3. 触发副作用 (Side Effect)
        if (success) {
            dispatchMenuChangeEvent("批量删除菜单: " + menuIds);
        }
    }

    // ==========================================
    // 私有辅助方法 (Private Helper Methods)
    // ==========================================

    /**
     * 派发菜单变更事件 (Dispatch Change Event)
     * <p>
     * 统一处理写操作后的缓存一致性维护。
     * <ol>
     * <li>清除 Redis 中的网关路由规则缓存。</li>
     * <li>发送 MQ 广播，通知集群中其他节点（或网关服务）重载内存中的权限配置。</li>
     * </ol>
     *
     * @param reason 变更原因 (用于日志审计)
     */
    private void dispatchMenuChangeEvent(String reason) {
        log.info("检测到菜单资源变更 [{}]，正在执行权限同步流程...", reason);

        // 步骤 A: 清除 Redis 权威缓存
        redisCache.deleteObject(RedisConstants.SYS_AUTH_RULES_KEY);

        // 步骤 B: 发送 RocketMQ 广播消息 (Payload 仅作为触发信号，具体内容不重要)
        // 订阅者收到消息后，应主动去 Redis 或 DB 拉取最新配置
        String signalPayload = "Refreshed at " + System.currentTimeMillis();
        rocketMQTemplate.convertAndSend(RocketMqConstants.TOPIC_SYS_BROADCAST + RocketMqConstants.TOPIC_TAG_SEPARATOR + RocketMqConstants.TAG_AUTH, signalPayload);
    }

    /**
     * 递归构建树形结构 - 入口 (Recursive Entry Point)
     *
     * @param menus 全量扁平菜单列表
     * @return 组装好的树形根节点列表
     */
    private List<SysMenu> buildTree(List<SysMenu> menus) {
        return menus.stream()
                // 筛选根节点 (约定：ParentId 为 null 或 0)
                .filter(menu -> menu.getParentId() == null || menu.getParentId() == 0)
                .peek(menu -> menu.setChildren(getChildren(menu, menus)))
                .collect(Collectors.toList());
    }

    /**
     * 递归构建树形结构 - 子节点查找 (Recursive Step)
     *
     * @param root     当前父节点
     * @param allMenus 全量菜单列表
     * @return 该父节点的直接子节点列表 (已递归组装好后代)
     */
    private List<SysMenu> getChildren(SysMenu root, List<SysMenu> allMenus) {
        return allMenus.stream()
                .filter(menu -> menu.getParentId() != null && menu.getParentId().equals(root.getId()))
                .peek(menu -> menu.setChildren(getChildren(menu, allMenus))) // 递归下探 (Drill Down)
                .collect(Collectors.toList());
    }
}