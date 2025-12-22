package com.nshj.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户-角色关联实体 (Junction Table Entity)
 * <p>
 * <b>架构定位：</b>
 * RBAC (Role-Based Access Control) 权限模型中的核心纽带。
 * 负责将 {@link SysUser} (身份主体) 与 {@link SysRole} (权限组) 进行解耦，
 * 将两者之间的 "多对多 (Many-to-Many)" 关系拆解为一对多关系。
 * <p>
 * <b>业务含义：</b>
 * 实现了 "一人多岗" 的灵活授权机制（例如：某用户既是 "销售经理" 也是 "系统管理员"）。
 * <br>对应数据库表：{@code sys_user_role}
 *
 * @author nshj
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_role")
public class SysUserRole implements Serializable {

    /**
     * 用户 ID (Foreign Key)
     * <p>
     * 指向 {@link SysUser#getId()}。
     * <br>代表被授权的主体（Subject/Who）。
     */
    private Long userId;

    /**
     * 角色 ID (Foreign Key)
     * <p>
     * 指向 {@link SysRole#getId()}。
     * <br>代表赋予该用户的身份或岗位（Role/What）。
     */
    private Long roleId;
}