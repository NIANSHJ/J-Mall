package com.nshj.mall.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 系统用户实体 (Identity Core Entity)
 * <p>
 * <b>架构定位：</b>
 * 平台身份认证体系中的核心主体 (Subject)。
 * <p>
 * <b>数据契约：</b>
 * <ul>
 * <li><b>持久化：</b> 映射数据库表 {@code sys_user}。</li>
 * <li><b>敏感性：</b> 包含高密级凭证数据 (密码)。在 Controller 层向前端返回数据时，
 * 务必将其置为 {@code null} 或使用 VO 对象转换，防止哈希值泄露。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /**
     * 登录账号 (Principal Identifier)
     * <p>
     * <b>业务约束：</b> 系统内的全局唯一标识 (Unique Key)，不支持重复。
     * <br><b>用途：</b> Spring Security `UserDetailsService` 加载用户时的检索凭证。
     */
    private String username;

    /**
     * 登录凭证 (Credential Secret)
     * <p>
     * <b>存储规范：</b> <font color="red">严禁存储明文。</font>
     * 数据库中必须存储经过加盐哈希 (Salted Hash) 处理的密文。
     * <br><b>算法：</b> 通常采用 BCrypt 算法 (Spring Security 默认)。
     */
    private String password;

    /**
     * 用户显示名称 (Display Name)
     * <p>
     * <b>用途：</b> 在 UI 界面（如顶部欢迎语、表格列）中展示的友好名称。
     * 不同于 {@code username}，该字段允许重复且支持特殊字符 (如中文)。
     */
    private String nickname;

    /**
     * 移动联系方式 (Mobile Contact)
     * <p>
     * <b>扩展能力：</b> 可作为第二认证因子 (2FA) 或用于短信验证码登录、密码找回流程。
     */
    private String phone;

    /**
     * 头像资源路径 (Avatar URI)
     * <p>
     * 存储用户自定义头像的相对路径或 OSS (对象存储) 的完整链接。
     */
    private String avatar;

    /**
     * 账户生命周期状态 (Account Lifecycle)
     * <p>
     * <b>逻辑控制：</b>
     * <ul>
     * <li><b>1:</b> 激活 (Active) - 允许正常认证与授权。</li>
     * <li><b>0:</b> 冻结 (Suspended) - 禁止登录，通常保留历史数据关联。</li>
     * </ul>
     * 对应 Spring Security 中的 {@code isEnabled()} 判断逻辑。
     */
    private Integer status;

    /**
     * 关联角色 ID 集合 (Data Transfer Field)
     * <p>
     * <b>架构特性：</b> 瞬态属性 (Transient)。
     * <br><b>注解：</b> {@code @TableField(exist = false)} 表明该字段不对应数据库列。
     * <br><b>用途：</b> 仅作为数据传输 (DTO) 使用。在用户 "新增/编辑" 接口中，
     * 前端将选中的角色 ID 列表通过此字段传入，Service 层处理后写入 `sys_user_role` 中间表。
     */
    @TableField(exist = false)
    private List<Long> roleIds;
}