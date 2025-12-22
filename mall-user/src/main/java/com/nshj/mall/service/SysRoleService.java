package com.nshj.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nshj.mall.entity.SysRole;

import java.util.List;

/**
 * 角色领域服务契约 (Role Domain Service)
 * <p>
 * <b>职责边界：</b>
 * <ol>
 * <li><b>元数据管理：</b> 维护角色定义 (Name, Key, SortOrder)。</li>
 * <li><b>授权管理 (Authorization)：</b> 维护 {@code SysRole} (主体) 与 {@code SysMenu} (资源) 的聚合关系 (sys_role_menu)。</li>
 * <li><b>鉴权支持 (Authentication)：</b> 为用户登录流程提供 "当前用户具备哪些角色" 的查询能力。</li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 检索分页数据
     *
     * @param pageNum  当前页码
     * @param pageSize 页容量
     * @param name     名称过滤器
     * @return 分页对象
     */
    Page<SysRole> getRolePage(Integer pageNum, Integer pageSize, String name);

    /**
     * 创建角色 (含关联菜单)
     *
     * @param sysRole 角色实体 (包含 menuIds)
     */
    void addRole(SysRole sysRole);

    /**
     * 更新角色 (含关联菜单)
     * <p>
     * <b>注意：</b> 修改成功后需处理缓存一致性问题。
     *
     * @param sysRole 角色实体 (包含 menuIds)
     */
    void updateRole(SysRole sysRole);

    /**
     * 批量删除角色
     * <p>
     * <b>级联清除 (Cascading Delete)：</b>
     * <ol>
     * <li>删除角色主体。</li>
     * <li>删除角色-菜单关联数据 ({@code sys_role_menu})。</li>
     * <li>删除用户-角色关联数据 ({@code sys_user_role})。</li>
     * </ol>
     *
     * @param roleIds 待删除的角色 ID 集合
     */
    void removeRoleBatchByIds(List<Long> roleIds);

    /**
     * 查询关联菜单索引
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表 (Pure IDs)
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /**
     * 查询用户具备的角色标识 (For Security)
     * <p>
     * <b>场景：</b> {@code UserDetailsServiceImpl} 加载用户信息时调用。
     * <br><b>返回示例：</b> {@code ["admin", "common", "tester"]}
     *
     * @param userId 用户 ID
     * @return 角色 Key 集合
     */
    List<String> getRoleKeysByUserId(Long userId);
}