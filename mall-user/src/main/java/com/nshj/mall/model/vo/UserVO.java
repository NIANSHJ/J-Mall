package com.nshj.mall.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户基础信息视图对象 (View Object)
 * <p>
 * <b>架构定位：</b>
 * 位于表现层 (Presentation Layer) 的输出端，专门用于向前端客户端（Web/App）响应数据。
 * <p>
 * <b>设计原则 - 信息清洗 (Data Sanitization)：</b>
 * <ul>
 * <li><b>安全隔离：</b> 严格剔除 {@code password}, {@code salt} 等高敏感凭证字段，从物理层面防止后端向前端泄露机密。</li>
 * <li><b>格式统一：</b> 对日期、枚举等复杂类型进行预处理 (如将 Date 转为格式化后的 String)，确保前端展示的一致性，减轻前端逻辑负担。</li>
 * <li><b>隐私脱敏：</b> 承载经过掩码处理 (Masking) 的敏感数据 (如手机号)，保护用户隐私。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Data
@Schema(description = "用户基础信息响应数据")
public class UserVO implements Serializable {

    /**
     * 用户唯一索引 (Surrogate Key)
     * <p>
     * <b>用途：</b> 前端进行数据绑定、路由跳转 ({@code /user/profile/:id}) 或发起编辑/删除 API 请求时的唯一句柄。
     * <br><b>安全策略：</b> 若系统对 ID 遍历攻击敏感，此处建议升级为加密后的 HashID。
     */
    @Schema(description = "用户ID (主键)", example = "10001")
    private Long id;

    /**
     * 登录账号 (Identity Principal)
     * <p>
     * <b>业务约束：</b> 系统内的不可变 (Immutable) 标识符。
     * <br>仅用于前端展示账号名称，不可作为修改业务的 Key（修改应依赖 ID）。
     */
    @Schema(description = "登录账号", example = "admin")
    private String username;

    /**
     * 用户显示名称 (Display Name)
     * <p>
     * <b>用途：</b> 界面中用于称呼用户的友好文本，如 "欢迎您，[昵称]"。
     */
    @Schema(description = "用户昵称", example = "超级管理员")
    private String nickname;

    /**
     * 联系方式
     * <p>
     * <b>隐私处理：</b>
     * <font color="red">注意：</font> 该字段在返回给前端前，通常建议在 Service 或 Controller 层进行<b>脱敏处理</b>。
     * <br><b>示例：</b> {@code 138****8000}
     */
    @Schema(description = "手机号码", example = "13800138000")
    private String phone;

    /**
     * 头像资源链接 (Avatar URI)
     * <p>
     * <b>数据格式：</b> 通常为完整的 CDN 链接或 OSS 访问路径，方便前端 {@code <img>} 标签直接渲染，无需拼接。
     */
    @Schema(description = "头像地址", example = "https://oss.example.com/avatar/user_1.png")
    private String avatar;

    /**
     * 账户当前状态
     * <p>
     * <b>枚举定义：</b> 1: 正常 (Active), 0: 禁用 (Disabled)。
     * <p>
     * <b>前端应用：</b>
     * 用于控制列表页的状态标签颜色 (如 Green/Red) 或操作按钮的可用性 (Disable/Enable)。
     */
    @Schema(description = "帐号状态 (1=正常, 0=禁用)", example = "1")
    private Integer status;

    /**
     * 注册时间 (Formatted String)
     * <p>
     * <b>格式契约：</b> {@code yyyy-MM-dd HH:mm:ss}
     * <p>
     * <b>架构决策：</b>
     * 使用 {@code String} 而非 {@code Date} 类型。
     * <br><b>目的：</b> 将时间格式化的逻辑收敛于后端，避免因客户端设备时区 (TimeZone) 差异导致显示时间不一致的问题。
     */
    @Schema(description = "创建时间", example = "2023-12-01 12:00:00")
    private String createTime;
}