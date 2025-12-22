package com.nshj.mall.service;

import com.nshj.mall.model.dto.UserLoginDTO;

import java.util.Map;

/**
 * 认证编排服务 (Authentication Orchestration Service)
 * <p>
 * <b>职责边界：</b>
 * 负责协调用户域 (User Domain) 与安全域 (Security Domain) 的交互。
 * <br>本层通常不直接操作数据库表，而是调用 {@code SysUserService} 获取数据，并调用 {@code JwtUtils} 处理令牌。
 * <p>
 * <b>核心能力：</b>
 * <ol>
 * <li><b>凭证比对 (Credential Matching):</b> 验证账号密码正确性。</li>
 * <li><b>令牌签发 (Token Issuance):</b> 生成 JWT 字符串。</li>
 * <li><b>会话状态管理 (Session State):</b> 处理 Redis 中的登录态缓存与黑名单。</li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * 执行登录认证流程
     * <p>
     * <b>原子操作序列：</b>
     * <ol>
     * <li>调用 Spring Security 的 {@code AuthenticationManager} 进行身份验证。</li>
     * <li>验证通过后，获取 {@code LoginUser} 主体信息。</li>
     * <li>生成 JWT 令牌，并将用户信息缓存至 Redis (Key: {@code login_tokens:uuid})。</li>
     * </ol>
     *
     * @param loginDTO 用户提交的原始凭证
     * @return 令牌键值对 (Key: "token", Value: "Bearer xxxx")
     */
    Map<String, String> login(UserLoginDTO loginDTO);

    /**
     * 执行登出注销流程
     * <p>
     * <b>副作用 (Side Effect)：</b>
     * 1. 获取当前请求上下文中的用户 ID 或 Token。
     * 2. 立即从 Redis 中驱逐当前用户的会话数据 (Cache Eviction)。
     * 3. (可选) 将 Token 加入黑名单以防止重放攻击。
     */
    void logout();
}