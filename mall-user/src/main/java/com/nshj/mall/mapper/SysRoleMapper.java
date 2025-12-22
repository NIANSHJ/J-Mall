package com.nshj.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nshj.mall.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 角色信息数据访问接口 (Role Data Access Object)
 * <p>
 * <b>架构定位：</b>
 * 位于持久层 (Persistence Layer)，主要负责 {@link SysRole} 实体与数据库 {@code sys_role} 表之间的
 * 对象关系映射 (O/R Mapping) 及数据持久化交互。
 * <p>
 * <b>核心职能：</b>
 * <ul>
 * <li><b>基础 CRUD：</b> 继承 {@link BaseMapper}，自动装配标准化的增删改查能力。</li>
 * <li><b>RBAC 支撑：</b> 提供基于多表关联 (Join) 的复杂查询能力，支撑基于角色的访问控制模型。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询指定用户所拥有的角色标识集合 (Role Keys)
     * <p>
     * <b>数据契约：</b>
     * 执行跨表关联查询 ({@code sys_role} INNER JOIN {@code sys_user_role})，获取目标用户关联的所有角色标识。
     * <p>
     * <b>过滤策略：</b>
     * <ul>
     * <li><b>关联匹配：</b> 严格匹配 {@code user_id}。</li>
     * <li><b>状态过滤：</b> 仅返回状态正常 ({@code status=0/1} 根据业务定义，通常为启用状态) 且未被逻辑删除的角色，确保权限的有效性。</li>
     * </ul>
     * <p>
     * <b>业务场景：</b>
     * 此方法通常由安全框架 (如 Spring Security) 在用户登录或鉴权时调用，
     * 用于构建用户的 {@code GrantedAuthority} 权限集合，支撑 {@code @PreAuthorize("hasRole('admin')")} 等注解的校验逻辑。
     *
     * @param userId 目标用户的唯一标识 (主键)
     * @return 角色权限字符串集合 (例如：{@code ["admin", "common", "tester"]})
     */
    List<String> selectRoleKeysByUserId(Long userId);
}