package com.nshj.mall.config;

import com.nshj.mall.filter.JwtAuthenticationTokenFilter;
import com.nshj.mall.handler.JwtAuthenticationEntryPoint;
import com.nshj.mall.handler.RestfulAccessDeniedHandler;
import com.nshj.mall.manager.DynamicAuthorizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全策略编排配置
 * <p>
 * 核心安全架构配置类，基于 "Chain of Responsibility" (责任链) 模式。
 * 负责构建应用的防火墙，定义认证(Authentication)与鉴权(Authorization)的全局规则。
 * <p>
 * <b>架构特征：</b>
 * <ul>
 * <li><b>无状态 (Stateless):</b> 禁用 Session，完全依赖 JWT 进行身份维系。</li>
 * <li><b>动态鉴权 (Dynamic RBAC):</b> 权限规则不硬编码，委托给动态管理器实时判定。</li>
 * <li><b>零信任前置:</b> 默认拦截所有请求，仅放行明确定义的白名单。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final RestfulAccessDeniedHandler restfulAccessDeniedHandler;
    private final DynamicAuthorizationManager dynamicAuthorizationManager;
    private final JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    /**
     * 静态资源与公共接口白名单
     * <p>
     * 包含 Swagger 文档、公共资源等无需经过安检的路径。
     */
    private static final String[] WHITE_LIST_URLS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    /**
     * 安全过滤器链 (Security Filter Chain)
     * <p>
     * 这是 Spring Security 的心脏，定义了 HTTP 请求如何一步步经过安全检查。
     *
     * @param http HTTP 安全配置构建器
     * @return 构建完成的过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF (跨站请求伪造) 防护
                // 原因：系统采用无状态的 JWT 机制，不依赖 Cookie 进行 Session 认证，
                // 也就是浏览器不会自动携带凭证，因此天然免疫 CSRF 攻击，无需开启防护。
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 启用 CORS (跨域资源共享)
                // 策略：使用默认配置 (Customizer.withDefaults())。
                // 这将引导 Security 寻找 CorsConfigurationSource Bean，与 MVC 全局跨域配置对齐。
                .cors(Customizer.withDefaults())

                // 3. 异常处理 (Exception Handling)
                // 指定认证失败时的处理逻辑（如：Token 无效、Token 过期）。
                // 默认行为是重定向到 /login，API 模式下需改为返回 401 JSON。
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(restfulAccessDeniedHandler)
                )

                // 4. 会话管理 (Session Management)
                // 策略：STATELESS (无状态)。
                // 明确告知 Spring Security 不要创建 HttpSession，也不要使用 Session 获取 SecurityContext。
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 5. 请求鉴权 (Authorization)
                // 定义 URL 的访问控制策略。
                .authorizeHttpRequests(auth -> auth
                        // 放行 Swagger 等静态资源
                        .requestMatchers(WHITE_LIST_URLS).permitAll()
                        // 放行登录、注册等认证端点
                        .requestMatchers("/auth/**").permitAll()
                        // 核心策略：剩余所有请求，委托给动态权限管理器 (DynamicAuthorizationManager)
                        // 实现了配置即生效的数据库级权限控制。
                        .anyRequest().access(dynamicAuthorizationManager)
                )

                // 6. 植入自定义过滤器
                // 将 JWT 认证过滤器插入到 UsernamePasswordAuthenticationFilter 之前。
                // 确保请求在进入后续逻辑前，SecurityContext 已被 Token 中的用户信息填充。
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码哈希编码器 (Password Hashing)
     * <p>
     * <b>算法：</b> BCrypt
     * <br>
     * <b>特性：</b> 强哈希、自带盐值 (Salt)、抗彩虹表攻击。
     * 即使两个用户的明文密码相同，生成的密文也不同。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器 (Authentication Manager)
     * <p>
     * 导出 Spring Security 核心的认证 Bean。
     * 业务层 (LoginService) 需要注入此 Bean 来手动触发 username/password 的校验流程。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}