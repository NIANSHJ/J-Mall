package com.nshj.mall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nshj.mall.entity.SysRoleMenu;

/**
 * 角色-菜单关联中间层接口 (Role-Menu Association Interface)
 * <p>
 * <b>架构定位：</b>
 * 属于基础设施层服务。专注于维护 {@code sys_role_menu} 中间表，
 * 实现 "角色(Role)" 与 "菜单(Menu)" 之间的 M:N (多对多) 映射关系。
 * <p>
 * <b>核心职能：</b>
 * 作为 {@link SysRoleService} 的下游支撑组件，负责落地 "权限分配" 业务逻辑，
 * 确保角色与资源权限（API/路由）的绑定关系持久化。
 *
 * @author nshj
 * @since 1.0.0
 */
public interface SysRoleMenuService extends IService<SysRoleMenu> {
}