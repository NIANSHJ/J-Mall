package com.nshj.mall.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nshj.mall.response.Result;
import com.nshj.mall.response.ReturnCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 鉴权拒绝处理器 (Access Denied Handler)
 * <p>
 * <b>架构定位：</b>
 * Spring Security 异常处理体系中负责 "权限不足 (Forbidden)" 场景的终端处理器。
 * <p>
 * <b>核心职责：</b>
 * 当已认证的用户 (Authenticated User) 试图访问其角色或权限范围之外的资源时，
 * 此类负责拦截默认的错误页跳转，并返回符合 RESTful API 契约的 403 JSON 响应。
 *
 * @author nshj
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestfulAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * 执行拒绝访问响应 (Handle Access Denied)
     * <p>
     * <b>处理流程：</b>
     * <ol>
     * <li><b>审计日志：</b> 记录无权访问的具体路径与用户信息，便于安全审计与入侵检测。</li>
     * <li><b>协议头设置：</b> 明确标记响应类型为 JSON，状态码设为 HTTP 403 (Forbidden)。</li>
     * <li><b>报文构建：</b> 封装标准 {@link Result} 响应体，提示"权限不足"。</li>
     * <li><b>序列化输出：</b> 绕过 Spring MVC 视图解析器，直接向 Servlet 响应流写入 JSON 数据。</li>
     * </ol>
     *
     * @param request               触发异常的 HTTP 请求
     * @param response              HTTP 响应对象
     * @param accessDeniedException 捕获到的权限拒绝异常 (通常由 AuthorizationManager 抛出)
     * @throws IOException 当响应流写入失败时抛出
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        // Step 1: 安全审计日志 (Audit Logging)
        // 记录谁(User)在什么时候试图访问哪个受限资源(Path)，这是排查权限配置错误或发现恶意扫描的关键线索
        log.warn("[鉴权拦截] 用户: {}, 试图访问未授权接口: {}",
                request.getRemoteUser(), request.getRequestURI());

        // Step 2: 声明响应协议 (HTTP 403 + JSON)
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // Step 3: 动态构建响应体
        // 直接使用枚举定义的 RC403 状态码 (假设 ReturnCode 中已定义 RC403)
        // 如果没有定义 RC403，可改为: Result.fail(403, "您的权限不足，无法访问该资源");
        Result<String> result = Result.fail(ReturnCode.RC403);

        // Step 4: 手动序列化并写入响应流 (Manual Serialization)
        // 保持与 EntryPoint 一致的高效写入方式
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().write(json);
    }
}