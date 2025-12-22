package com.nshj.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 角色-菜单关联实体 (Junction Table Entity)
 * <p>
 * <b>架构定位：</b>
 * 关系型数据库设计的标准化产物。用于将 {@link SysRole} 与 {@link SysMenu} 之间的
 * "多对多 (Many-to-Many)" 关系解耦为两个 "一对多 (One-to-Many)" 关系。
 * <p>
 * <b>数据特征：</b>
 * 这是一个纯粹的关联表 (Association Table)，仅包含联合主键/外键，不承载额外的业务属性。
 * <br>对应数据库表：{@code sys_role_menu}
 *
 * @author nshj
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    /**
     * 角色 ID (Foreign Key)
     * <p>
     * 指向 {@link SysRole#getId()}。
     * <br>代表 RBAC 模型中的授权主体（Who）。
     */
    private Long roleId;

    /**
     * 菜单/资源 ID (Foreign Key)
     * <p>
     * 指向 {@link SysMenu#getId()}。
     * <br>代表 RBAC 模型中的授权客体（What）。
     */
    private Long menuId;
}