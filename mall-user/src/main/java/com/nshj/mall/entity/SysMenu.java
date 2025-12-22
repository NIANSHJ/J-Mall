package com.nshj.mall.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统菜单与权限资源实体
 * <p>
 * <b>核心定义：</b>
 * 本类是 RBAC (Role-Based Access Control) 权限模型中的最小控制单元，
 * 对应数据库表 {@code sys_menu}。它统一抽象了系统中的“菜单”、“按钮”与“API 接口资源”。
 * <p>
 * <b>功能维度：</b>
 * <ul>
 * <li><b>视图层 (Frontend):</b> 定义左侧菜单栏的树形结构、路由路径以及按钮级别的显隐控制 ({@code perms})。</li>
 * <li><b>安全层 (Backend):</b> 定义后端接口的访问约束 ({@code apiPath} + {@code requestMethod})，供动态鉴权过滤器使用。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    /**
     * 资源展示名称
     * <p>
     * 在前端侧边导航栏、面包屑或权限分配树（TreeSelect）中显示的文本标签。
     */
    private String menuName;

    /**
     * 父级资源 ID
     * <p>
     * 采用邻接表 (Adjacency List) 方式维护树形结构。
     * <br><b>约定：</b> 根节点的父 ID 通常为 0。
     */
    private Long parentId;

    /**
     * 权限标识符 (Permission Key)
     * <p>
     * <b>核心字段：</b> 对应 Spring Security 中的 {@code SimpleGrantedAuthority}。
     * <br><b>格式规范：</b> 通常采用冒号分隔的层级命名法，如 {@code system:user:add}、{@code order:list:export}。
     * <br><b>用途：</b>
     * <ul>
     * <li>前端：Vue/React 指令控制按钮显隐 ({@code v-if="hasPerm('user:add')"})。</li>
     * <li>后端：方法级安全注解控制 ({@code @PreAuthorize("@ss.hasPerm('user:add')")})。</li>
     * </ul>
     */
    private String perms;

    /**
     * 关联接口路径 (API Endpoint)
     * <p>
     * 定义该资源对应的后端请求路径规则，支持 AntPath 风格通配符。
     * <br><b>示例：</b> {@code /admin/system/user/**}
     * <br><b>用途：</b> 用于网关或拦截器层面的动态 URL 鉴权（无需硬编码 Controller 注解）。
     */
    private String apiPath;

    /**
     * 接口请求谓词 (HTTP Method)
     * <p>
     * 配合 {@code apiPath} 字段，精确限定资源的访问方式。
     * <br><b>取值范围：</b> GET, POST, PUT, DELETE, ALL (或 null 表示不限制)。
     */
    private String requestMethod;

    /**
     * 资源状态
     * <p>
     * <b>枚举定义：</b> 1=正常 (Enabled), 0=停用 (Disabled)。
     * <br>当状态为停用时，该菜单不显示，且关联的 API 权限自动失效。
     */
    private Integer status;

    /**
     * 排序权重
     * <p>
     * 控制同级菜单在侧边栏的显示顺序。
     * <br><b>规则：</b> 数值越小越靠前 (升序 Ascending)。
     */
    private Integer sortOrder;

    /**
     * 子节点集合 (Tree Structure)
     * <p>
     * <b>非数据库字段：</b> {@code @TableField(exist = false)}
     * <br>仅在业务层进行“列表转树 (List to Tree)”算法处理时填充此字段，用于前端递归渲染菜单组件。
     */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}