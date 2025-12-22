package com.nshj.mall.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录认证传输对象 (Data Transfer Object)
 * <p>
 * <b>架构定位：</b>
 * 位于表现层 (Presentation Layer)，充当外部请求进入业务核心的第一道防线。
 * <p>
 * <b>安全策略 (Fail-Fast):</b>
 * 利用 JSR-303/Bean Validation 规范，在请求到达 Controller 方法体或查询数据库之前，
 * 强制拦截格式非法的参数。
 * <br><b>收益：</b> 阻断恶意扫描，降低数据库无效 I/O 压力，规避基础 SQL 注入风险。
 *
 * @author nshj
 * @since 1.0.0
 */
@Data
@Schema(description = "用户登录鉴权请求参数")
public class UserLoginDTO implements Serializable {

    /**
     * 登录账号 (Principal Identifier)
     * <p>
     * <b>校验规则：</b> 采用白名单 (Whitelist) 机制。
     * <br>仅允许字母开头，后接字母、数字或下划线。
     * <p>
     * <b>设计目的：</b>
     * <ol>
     * <li><b>安全防御：</b> 从源头杜绝特殊字符（如单引号、分号、注释符）输入，极大降低 SQL 注入攻击面。</li>
     * <li><b>性能优化：</b> 格式不符直接在网关或 Filter 层拒绝，避免无效的数据库查询操作。</li>
     * </ol>
     */
    @Schema(description = "登录账号 (字母开头，允许字母数字下划线，4-20位)", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,19}$",
            message = "账号格式非法：须以字母开头，仅包含字母/数字/下划线，长度4-20位")
    private String username;

    /**
     * 登录凭证 (Credential Secret)
     * <p>
     * <b>校验规则：</b> 仅校验非空与长度，<b>不校验</b>复杂度。
     * <p>
     * <b>业务考量：</b>
     * 登录接口应保持宽容度，允许历史遗留的弱密码或管理员重置后的简单初始密码通过验证。
     * <br>密码强度的严格校验（如大小写+数字+符号）应收敛于 "用户注册" 或 "修改密码" 场景。
     */
    @Schema(description = "登录密码 (明文)", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度范围为 6-64 位")
    private String password;
}