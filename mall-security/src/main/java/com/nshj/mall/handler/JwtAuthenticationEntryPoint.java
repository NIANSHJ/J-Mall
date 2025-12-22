package com.nshj.mall.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nshj.mall.response.Result;
import com.nshj.mall.response.ReturnCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 认证失败网关接入点 (Authentication Failure Endpoint)
 * <p>
 * <b>架构定位：</b>
 * Spring Security 异常过滤链 (Exception Translation Filter) 的终端处理节点。
 * <p>
 * <b>核心职责：</b>
 * 当匿名用户试图访问受保护资源 (Protected Resource) 失败时，此类负责中断请求流程，
 * 并将系统内部的 {@link AuthenticationException} 转化为符合 RESTful API 契约的 401 JSON 响应。
 *
 * @author nshj
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 执行认证质询 (Commence Authentication Challenge)
     * <p>
     * <b>处理流程：</b>
     * <ol>
     * <li><b>协议头设置：</b> 明确标记响应类型为 JSON，状态码设为 HTTP 401 (Unauthorized)。</li>
     * <li><b>上下文提取：</b> 尝试读取过滤器链 (Filter Chain) 上游传递的精确错误描述 (如"互踢"或"Token过期")。</li>
     * <li><b>报文构建：</b> 封装标准 {@link Result} 响应体。</li>
     * <li><b>序列化输出：</b> 绕过 Spring MVC 视图解析器，直接向 Servlet 响应流写入 JSON 数据。</li>
     * </ol>
     *
     * @param request       触发异常的 HTTP 请求
     * @param response      HTTP 响应对象
     * @param authException 捕获到的认证异常 (通常由 Security 框架自动抛出)
     * @throws IOException 当响应流写入失败时抛出
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        // Step 1: 声明响应协议 (HTTP 401 + JSON)
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Step 2: 提取上游过滤器透传的业务错误细节
        // 机制：JwtAuthenticationTokenFilter 虽放行了异常请求，但通过 Request Attribute 传递了具体死因
        String errorMsg = (String) (request.getAttribute("AUTH_ERROR_MSG"));

        Result<String> result;

        // Step 3: 动态构建响应体
        if (StringUtils.hasText(errorMsg)) {
            // Case A: 明确的业务阻断 (如: "您的账号已在其他设备登录")
            // 此时虽是 401 状态，但给前端具体的 Business Message
            result = Result.fail(ReturnCode.RC401.getCode(), errorMsg);
        } else {
            // Case B: 标准的未授权访问 (如: Token 缺失、签名错误、非法伪造)
            // 使用默认提示 "Token 已失效"
            result = Result.fail(ReturnCode.RC401);
        }

        // Step 4: 手动序列化并写入响应流 (Manual Serialization)
        // 注意：此处不经过 Controller，因此无法使用 @ResponseBody，需手动调用 Jackson
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().write(json);
    }
}