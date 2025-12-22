package com.nshj.mall.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色信息实体 (RBAC Core Node)
 * <p>
 * <b>架构定位：</b>
 * RBAC (Role-Based Access Control) 权限模型中的逻辑中枢与聚合根。
 * <p>
 * <b>模型关系：</b>
 * <ul>
 * <li><b>上游 (Users):</b> 多对多关联用户。一个用户可具备多个角色身份（如既是“部门经理”又是“考勤员”）。</li>
 * <li><b>下游 (Permissions):</b> 多对多关联菜单/资源。一个角色本质上是一组特定权限的具象化集合。</li>
 * </ul>
 * 对应数据库表：{@code sys_role}
 *
 * @author nshj
 * @see SysMenu
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /**
     * 角色显示名称
     * <p>
     * <b>用途：</b> 面向终端用户的业务标识，用于前端下拉框、列表展示。
     * <br><b>示例：</b> "超级管理员", "财务专员", "运营经理"
     */
    private String name;

    /**
     * 角色权限字符串 (Authority Key)
     * <p>
     * <b>用途：</b> 面向程序的唯一逻辑标识，用于后端代码中的硬编码判断或注解鉴权。
     * <p>
     * <b>最佳实践：</b>
     * 建议遵循 Spring Security 规范。若使用 {@code @PreAuthorize("hasRole('ADMIN')")}，
     * 则此字段建议以 standard prefix {@code ROLE_} 开头（例如 {@code ROLE_ADMIN}），
     * 或在加载 `UserDetails` 时统一追加前缀。
     */
    private String roleKey;

    /**
     * 角色状态
     * <p>
     * <b>逻辑影响：</b>
     * 当状态为 0 (停用) 时，系统在构建 SecurityContext 时应自动过滤掉该角色。
     * 即使用户关联了该角色，其实际拥有的权限集合也应视作空，从而实现“一键熔断”该角色的所有特权。
     * <br><b>枚举值：</b> 1: 正常, 0: 停用
     */
    private Integer status;

    /**
     * 显示顺序
     * <p>
     * 控制角色在管理后台列表或分配时的排序权重。
     * <br><b>规则：</b> 数值越小越靠前 (Ascending)。
     */
    private Integer sortOrder;

    /**
     * 关联菜单 ID 集合 (DTO Field)
     * <p>
     * <b>非持久化字段：</b> {@code @TableField(exist = false)}
     * <p>
     * <b>用途：</b>
     * 仅用于角色创建/更新接口的数据传输 (Data Transfer)。
     * 前端通过勾选树形菜单组件，将选中的 {@code menuId} 列表封装在此字段中传给后端 Service 层进行关联表 (`sys_role_menu`) 的维护。
     */
    @TableField(exist = false)
    private List<Long> menuIds;
}