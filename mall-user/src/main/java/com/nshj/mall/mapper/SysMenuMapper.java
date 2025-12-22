package com.nshj.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nshj.mall.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 菜单资源数据访问接口 (Menu Data Access Object)
 * <p>
 * <b>架构定位：</b>
 * 位于持久层 (Persistence Layer)，直接对接数据库的 {@code sys_menu} 表。
 * <p>
 * <b>核心职责：</b>
 * 负责 {@link SysMenu} 实体的数据持久化，并作为 RBAC (Role-Based Access Control) 权限模型中
 * “资源”维度的核心查询组件，提供权限聚合与元数据加载能力。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 * <li><b>基础能力：</b> 继承 {@link BaseMapper}，提供标准的单表增删改查。</li>
 * <li><b>权限聚合：</b> 支持跨多表 (User-Role-Menu) 的复杂关联查询，计算用户的最终权限集。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询指定用户持有的所有权限标识 (Permission Identifiers)
     * <p>
     * <b>数据契约：</b>
     * 执行全链路关联查询 ({@code sys_user} -> {@code sys_user_role} -> {@code sys_role} -> {@code sys_role_menu} -> {@code sys_menu})。
     * <p>
     * <b>过滤策略：</b>
     * <ul>
     * <li><b>状态约束：</b> 仅包含 菜单启用 ({@code status=1}) 且 角色启用 ({@code status=1}) 的有效权限。</li>
     * <li><b>去重处理：</b> 结果集执行 {@code DISTINCT} 操作，消除因多角色重叠导致的权限冗余。</li>
     * <li><b>逻辑删除：</b> 自动过滤 {@code deleted=1} 的脏数据。</li>
     * </ul>
     * <p>
     * <b>业务场景：</b>
     * 核心鉴权方法。用户登录成功后，Security 框架调用此方法构建 {@code GrantedAuthority} 列表，
     * 用于前端按钮显隐控制及后端接口的 {@code @PreAuthorize} 鉴权。
     *
     * @param userId 目标用户 ID
     * @return 权限字符串集合 (例如：{@code ["system:user:add", "order:export"]})
     */
    List<String> selectPermsByUserId(Long userId);

    /**
     * 查询全量动态权限规则 (Dynamic Security Rules)
     * <p>
     * <b>数据契约：</b>
     * 查询所有需要鉴权的菜单资源 (通常类型为 API/接口)，提取其请求路径与权限标识的映射关系。
     * <p>
     * <b>性能考量：</b>
     * 仅查询关键字段 (如 {@code url/path}, {@code perms})，最小化网络传输与内存占用。
     * <p>
     * <b>业务场景：</b>
     * 用于应用启动或权限变更时，初始化网关 (Gateway) 或安全拦截器的动态鉴权配置，
     * 实现 "URL <-> 权限" 关系的动态装载。
     *
     * @return 包含路径与权限信息的菜单列表
     */
    List<SysMenu> selectMenuRules();
}