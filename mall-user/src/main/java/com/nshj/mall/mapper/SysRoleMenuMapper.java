package com.nshj.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nshj.mall.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色与菜单关联数据访问接口 (Role-Menu Association DAO)
 * <p>
 * <b>架构定位：</b>
 * 位于持久层 (Persistence Layer)，直接对接数据库的 {@code sys_role_menu} 中间表。
 * <p>
 * <b>核心职责：</b>
 * 负责维护 {@link com.nshj.mall.entity.SysRole} (角色) 与 {@link com.nshj.mall.entity.SysMenu} (菜单/权限) 之间的
 * <b>多对多 (M:N)</b> 映射关系，是 RBAC 权限控制模型中的核心关联节点。
 * <p>
 * <b>业务场景：</b>
 * <ul>
 * <li><b>权限分配 (Authorization):</b> 将具体的菜单功能或 API 权限授予特定角色。</li>
 * <li><b>关联维护:</b> 当角色发生变更或删除时，同步处理底层的权限映射记录。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}