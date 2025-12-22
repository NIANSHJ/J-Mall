package com.nshj.mall.filter;

import com.nshj.mall.constant.JwtConstants;
import com.nshj.mall.constant.RedisConstants;
import com.nshj.mall.model.security.LoginUser;
import com.nshj.mall.utils.JwtUtils;
import com.nshj.mall.utils.RedisCache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 身份认证过滤器 (Security Filter)
 * <p>
 * <b>架构定位：</b>
 * 位于 Spring Security 过滤器链的核心位置 (Before UsernamePasswordAuthenticationFilter)。
 * 它是无状态架构 (Stateless Architecture) 中连接 HTTP 协议与 Spring Security 上下文的唯一桥梁。
 * <p>
 * <b>核心职责：</b>
 * <ol>
 * <li><b>凭证提取 (Extraction):</b> 从 HTTP Header 中解析 Bearer Token。</li>
 * <li><b>会话校验 (Validation):</b> 结合 Redis 实现分布式会话校验与"互踢"机制。</li>
 * <li><b>上下文填充 (Population):</b> 将合法的 {@link LoginUser} 注入 {@link SecurityContextHolder}。</li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final RedisCache redisCache;

    /**
     * 过滤逻辑实现
     * <p>
     * <b>处理流程：</b>
     * <ol>
     * <li><b>前置检查：</b> 快速过滤非 Token 请求，直接放行（交给后续匿名过滤器处理）。</li>
     * <li><b>令牌解析：</b> 验证 JWT 签名完整性，提取 UserId 与会话指纹 (UUID)。</li>
     * <li><b>状态查询：</b> 访问 Redis 获取热点用户信息，验证账号状态。</li>
     * <li><b>并发控制：</b> 比对 Token UUID 与 Redis 中存储的最新 UUID，实现单端登录强制下线。</li>
     * <li><b>授权注入：</b> 构建 {@link UsernamePasswordAuthenticationToken} 并存入安全上下文，完成认证。</li>
     * </ol>
     *
     * @param request     当前 HTTP 请求
     * @param response    当前 HTTP 响应
     * @param filterChain 过滤器责任链
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(JwtConstants.TOKEN_HEADER);

        // Phase 1: 头部格式校验 (Bearer Schema Check)
        if (authHeader != null && authHeader.startsWith(JwtConstants.TOKEN_PREFIX)) {
            String authToken = authHeader.substring(JwtConstants.TOKEN_PREFIX.length());

            // Phase 2: JWT 解析 (CPU 密集型操作)
            String tokenUuid = jwtUtils.getUuidFromToken(authToken);
            Long userId = jwtUtils.getUserIdFromToken(authToken);

            // Phase 3: 认证状态校验 (避免重复认证)
            // 仅当 SecurityContext 为空且 Token 解析成功时才执行后续逻辑
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Phase 4: 分布式会话检索 (IO 密集型操作)
                String redisKey = RedisConstants.USER_TOKEN_KEY + userId;
                LoginUser loginUser = redisCache.getCacheObject(redisKey);

                if (loginUser != null) {
                    // Phase 5: 安全风控校验 (Security Risk Check)
                    // 核心逻辑：比对 JWT 携带的 UUID 与 Redis 中的最新 UUID
                    // 场景 A: tokenUuid 为空 -> 令牌格式错误或遭到篡改
                    // 场景 B: uuid 不匹配 -> 账号已在另一设备登录，当前 Token 被旧化 (互踢)
                    if (tokenUuid == null || !tokenUuid.equals(loginUser.getTokenUUID())) {
                        String errorMsg = (tokenUuid == null) ? "令牌校验失败" : "账号已在其他设备登录";

                        // 策略：不直接抛出异常，而是标记 Request 属性并降级为匿名用户放行。
                        // 最终由 AuthenticationEntryPoint 统一处理 401 响应。
                        request.setAttribute("AUTH_ERROR_MSG", errorMsg);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Phase 6: 构建认证令牌 (Authentication Token Construction)
                    // 包含：Principal(用户信息), Credentials(脱敏), Authorities(权限集合)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Phase 7: 上下文挂载 (Context Mounting)
                    // 至此，Spring Security 视当前请求为"已认证用户"
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        // Phase 8: 传递至下一节点 (Pass-through)
        // 无论认证是否成功，都必须放行。若未认证，后续鉴权过滤器会拦截访问受保护资源的请求。
        filterChain.doFilter(request, response);
    }
}