package com.nshj.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nshj.mall.entity.SysRole;
import com.nshj.mall.entity.SysRoleMenu;
import com.nshj.mall.entity.SysUserRole;
import com.nshj.mall.exception.BusinessException;
import com.nshj.mall.mapper.SysRoleMapper;
import com.nshj.mall.service.SysRoleMenuService;
import com.nshj.mall.service.SysRoleService;
import com.nshj.mall.service.SysUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色业务核心实现 (RBAC Core Implementation)
 * <p>
 * <b>技术实现策略：</b>
 * <ol>
 * <li><b>不可变性守卫 (Immutability Guard):</b> 硬编码拦截对 Super Admin (ID=1) 的修改与删除，防止系统权限体系崩塌。</li>
 * <li><b>全量替换策略 (Full Replacement):</b> 在分配菜单权限时，采用 "Delete All + Insert All" 方式。相比于计算 "新增哪些/删除哪些" 的 Diff 算法，这种方式在数据量较小时（菜单通常仅几百个）更稳健且代码更简洁。</li>
 * <li><b>引用完整性 (Referential Integrity):</b> 删除角色时，同步清理 {@code sys_user_role} 和 {@code sys_role_menu} 两张中间表，防止产生孤儿数据。</li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuService sysRoleMenuService;
    private final SysUserRoleService sysUserRoleService;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public Page<SysRole> getRolePage(Integer pageNum, Integer pageSize, String name) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(name), SysRole::getName, name)
                .orderByAsc(SysRole::getSortOrder) // UX优化：优先按前端配置的排序号显示
                .orderByAsc(SysRole::getId);       // 兜底排序：保证分页结果确定性

        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRole(SysRole sysRole) {
        // 1. 落库主表 (此时 MyBatis Plus 会自动回填 ID)
        this.save(sysRole);

        // 2. 校验是否选择了菜单
        if (CollectionUtils.isEmpty(sysRole.getMenuIds())) {
            return;
        }

        // 3. 构造关联数据
        Long roleId = sysRole.getId();
        List<SysRoleMenu> roleMenuList = sysRole.getMenuIds().stream()
                .map(menuId -> new SysRoleMenu(roleId, menuId))
                .collect(Collectors.toList());

        // 4. 批量插入关联表
        sysRoleMenuService.saveBatch(roleMenuList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(SysRole sysRole) {
        Long roleId = sysRole.getId();

        // 1. 系统守卫：禁止篡改超级管理员
        if (Long.valueOf(1L).equals(roleId)) {
            throw new BusinessException("操作拒绝：系统超级管理员权限不可变更");
        }

        // 2. 更新主表基础信息
        this.updateById(sysRole);

        // 3. 清理旧版权限 (物理删除)
        sysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));

        // 4. 注入新版权限 (若有)
        if (!CollectionUtils.isEmpty(sysRole.getMenuIds())) {
            List<SysRoleMenu> roleMenuList = sysRole.getMenuIds().stream()
                    .map(menuId -> new SysRoleMenu(roleId, menuId))
                    .collect(Collectors.toList());
            sysRoleMenuService.saveBatch(roleMenuList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRoleBatchByIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }

        // 1. 系统守卫：禁止删除超级管理员
        if (roleIds.contains(1L)) {
            throw new BusinessException("操作拒绝：系统超级管理员不可删除");
        }

        // 2. 级联清理：角色-菜单关联 (SysRoleMenu)
        sysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getRoleId, roleIds));

        // 3. 级联清理：用户-角色关联 (SysUserRole)
        sysUserRoleService.remove(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getRoleId, roleIds));

        // 4. 执行本体删除
        this.removeBatchByIds(roleIds);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        // 投影查询优化：只查 menu_id 字段，减少网络 I/O 开销
        return sysRoleMenuService.list(new LambdaQueryWrapper<SysRoleMenu>()
                        .select(SysRoleMenu::getMenuId)
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleKeysByUserId(Long userId) {
        // 跨表查询通常交由 XML Mapper 处理以获得更好的 SQL 性能
        return sysRoleMapper.selectRoleKeysByUserId(userId);
    }
}