package com.nshj.mall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nshj.mall.entity.SysMenu;

import java.util.List;

/**
 * 菜单资源领域服务 (Menu Domain Service)
 * <p>
 * <b>职责边界：</b>
 * <ol>
 * <li><b>资源管理：</b> 负责菜单元数据的全生命周期管理 (CRUD)。</li>
 * <li><b>视图转换：</b> 支持数据在 "扁平列表 (Flat)" 与 "层级树 (Tree)" 形态间的高效切换。</li>
 * <li><b>鉴权支持：</b> 计算用户权限标识 (Authorities) 并提供动态路由规则。</li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 检索扁平化菜单列表
     *
     * @param menuName 菜单名称关键字 (可选)
     * @return 按 {@code orderNum} 升序排列的实体列表
     */
    List<SysMenu> getMenuList(String menuName);

    /**
     * 创建菜单资源
     * <p>
     * <b>执行流程：</b>
     * 1. 持久化数据至数据库。
     * 2. (可选) 触发 EventPublish 或 Cache Evict，通知系统刷新权限。
     *
     * @param sysMenu 菜单实体
     */
    void createMenu(SysMenu sysMenu);

    /**
     * 更新菜单资源
     * <p>
     * <b>副作用：</b> 变更生效后，需确保相关的路由缓存被清理。
     *
     * @param sysMenu 菜单实体 (ID 必填)
     */
    void updateMenu(SysMenu sysMenu);

    /**
     * 获取用户授权标识集合 (Permission Set)
     * <p>
     * <b>核心契约：</b>
     * 基于 RBAC 模型计算 {@code User -> Role -> Menu} 的权限并集。
     * <br>结果用于构造 Spring Security 的 {@code SimpleGrantedAuthority} 对象。
     *
     * @param userId 登录用户 ID
     * @return 权限字符串集合 (例如: {@code ["system:user:add", "order:export"]})
     */
    List<String> getPermsByUserId(Long userId);

    /**
     * 构建全量菜单树 (In-Memory Tree Building)
     * <p>
     * <b>算法策略：</b>
     * 采用 "一次查询 + 内存组装" 策略。
     * 一次性加载全量有效菜单 (WHERE status=1)，通过递归或 Map 引用算法完成父子关系构建，避免 N+1 查询问题。
     *
     * @return 包含嵌套 {@code children} 的根节点集合
     */
    List<SysMenu> getTreeMenu();

    /**
     * 获取动态路由鉴权规则 (Access Control Rules)
     * <p>
     * <b>业务场景：</b>
     * 供网关 (Gateway) 或自定义 Security Filter 初始化使用，建立 {@code Request Path} 与 {@code Permission Key} 的映射关系。
     *
     * @return 包含路由规则的菜单列表
     */
    List<SysMenu> getMenuRules();

    /**
     * 执行批量删除业务
     * <p>
     * <b>完整性约束：</b>
     * 必须强制要求先移除所有子节点，才能删除父节点。
     *
     * @param menuIds 目标菜单 ID 集合
     */
    void removeMenuBatchByIds(List<Long> menuIds);
}