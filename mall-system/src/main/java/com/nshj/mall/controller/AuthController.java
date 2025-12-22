package com.nshj.mall.controller;

import com.nshj.mall.annotation.Auditable;
import com.nshj.mall.model.dto.UserLoginDTO;
import com.nshj.mall.response.Result;
import com.nshj.mall.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证授权网关 (Authentication Gateway)
 * <p>
 * <b>架构定位：</b>
 * 系统安全的 "第一道防线"，统一接管所有与身份凭证生命周期相关的入口流量（登录、注销、刷新令牌）。
 * <p>
 * <b>安全配置规约 (Security Config):</b>
 * <ul>
 * <li><b>Login 接口：</b> 必须在 Spring Security 过滤器链中设为 {@code .permitAll()}，允许匿名访问。</li>
 * <li><b>Logout 接口：</b> 虽为认证相关，但业务上通常要求用户持有 "有效 Token" 才能发起 "注销" 动作，因此需处于 SecurityContext 保护之下。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "00.认证中心", description = "身份验证与令牌生命周期管理")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录 (Login)
     * <p>
     * <b>交互协议：</b>
     * <ol>
     * <li>客户端发送 {@code username} + {@code password} (建议通过 HTTPS 传输)。</li>
     * <li>服务端通过 {@code AuthenticationManager} 校验凭证有效性。</li>
     * <li>校验通过后，签发标准 JWT 字符串 (Bearer Token)。</li>
     * </ol>
     *
     * @param loginDTO 登录数据传输对象 (包含账号、密码，已开启 @Validated 校验)
     * @return 包含 Token 和 TokenHead 的键值对 Map
     */
    @Operation(summary = "用户登录", description = "校验账号密码并颁发 Bearer Token")
    @PostMapping("/login")
    @Auditable(module = "认证模块", action = "登录")
    public Result<Map<String, String>> login(@RequestBody @Validated UserLoginDTO loginDTO) {
        // 委托 Service 层处理核心认证逻辑
        Map<String, String> tokenMap = authService.login(loginDTO);
        return Result.success(tokenMap);
    }

    /**
     * 退出登录 (Logout)
     * <p>
     * <b>实现原理：</b>
     * 由于 JWT 本质是无状态 (Stateless) 的，服务端无法像 Session 那样直接销毁。
     * <br>所谓的 "登出"，实际上是采用 <b>"黑名单机制"</b>：
     * 将当前 Token 的 JTI (唯一标识) 写入 Redis 黑名单，并设置与 Token 剩余有效期一致的过期时间。
     *
     * @return 无需返回数据，HTTP 200 即代表操作被接受
     */
    @Operation(summary = "退出登录", description = "将当前令牌加入黑名单，使其立即失效")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }
}