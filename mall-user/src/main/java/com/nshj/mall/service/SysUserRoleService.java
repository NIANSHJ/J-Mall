package com.nshj.mall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nshj.mall.entity.SysUserRole;

/**
 * 用户-角色关联中间层接口 (User-Role Association Interface)
 * <p>
 * <b>架构定位：</b>
 * 属于基础设施层服务。专注于维护 {@code sys_user_role} 中间表，
 * 实现 "用户(User)" 与 "角色(Role)" 之间的 M:N (多对多) 映射关系。
 * <p>
 * <b>核心职能：</b>
 * 作为 {@link SysUserService} 的下游支撑组件，利用 MyBatis Plus 提供的 {@code saveBatch} 能力，
 * 高效处理用户角色的批量分配与解绑。
 *
 * @author nshj
 * @since 1.0.0
 */
public interface SysUserRoleService extends IService<SysUserRole> {
}