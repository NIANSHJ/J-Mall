package com.nshj.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nshj.mall.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户与角色关联数据访问接口 (User-Role Relation DAO)
 * <p>
 * <b>架构定位：</b>
 * 位于持久层 (Persistence Layer)，直接对接数据库的 {@code sys_user_role} 中间表。
 * <p>
 * <b>核心职责：</b>
 * 负责维护 {@link com.nshj.mall.entity.SysUser} (用户) 与 {@link com.nshj.mall.entity.SysRole} (角色) 之间的
 * <b>多对多 (M:N)</b> 映射关系。
 * <p>
 * <b>业务场景：</b>
 * <ul>
 * <li><b>授权 (Grant):</b> 建立用户 ID 与角色 ID 的绑定记录。</li>
 * <li><b>撤权 (Revoke):</b> 物理删除关联记录，解除用户的角色身份。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}