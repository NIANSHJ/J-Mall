package com.nshj.mall.aspect;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nshj.mall.annotation.Auditable;
import com.nshj.mall.constant.LogConstants;
import com.nshj.mall.entity.AuditLog;
import com.nshj.mall.response.ReturnCode;
import com.nshj.mall.service.LogDispatchService;
import com.nshj.mall.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作审计切面 (Audit Log Aspect)
 * <p>
 * <b>架构定位：</b>
 * 位于接口适配层 (Interface Adapter Layer)。
 * 利用 Spring AOP 对标记了 {@link Auditable} 注解的业务方法进行透明拦截，实现业务逻辑与审计逻辑的彻底解耦。
 * <p>
 * <b>核心流程：</b>
 * 1. <b>前置采集：</b> 记录开始时间。
 * 2. <b>执行业务：</b> 执行目标方法 {@code point.proceed()}。
 * 3. <b>异常捕获：</b> 若业务抛出异常，记录错误堆栈并重新抛出（不影响事务回滚）。
 * 4. <b>后置组装：</b> 提取 HTTP 上下文 (IP, URL) 及 安全上下文 (User)。
 * 5. <b>异步分发：</b> 将日志对象投递至 MQ，确保主业务线程不被阻塞。
 *
 * @author nshj
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final LogDispatchService logDispatchService;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    /**
     * 环绕通知 (Around Advice)
     * <p>
     * 拦截所有标注了 {@code @Auditable} 的方法。
     *
     * @param point     连接点，用于执行目标方法
     * @param auditable 注解对象，用于获取 module/action 元数据
     * @return 业务方法执行结果
     */
    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint point, Auditable auditable) throws Throwable {
        Object result;
        long startTime = System.currentTimeMillis();

        // 1. 初始化日志骨架 (Metadata)
        AuditLog auditLog = new AuditLog();
        auditLog.setModule(auditable.module())
                .setAction(auditable.action())
                .setCreateTime(LocalDateTime.now());

        try {
            // 2. 执行目标业务方法
            // 若此处发生异常，将直接进入 catch 块
            result = point.proceed();

            // 业务执行成功
            auditLog.setCode(ReturnCode.RC200.getCode());
        } catch (Throwable e) {
            // 3. 业务执行失败
            // 记录错误状态码与异常摘要 (截取前5000字符防止存储溢出)
            auditLog.setCode(ReturnCode.RC500.getCode());
            auditLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 5000));
            // 必须重新抛出异常，否则上层 @Transactional 无法感知异常导致事务不回滚
            throw e;
        } finally {
            // 4. 最终处理 (无论成功失败均需记录日志)
            try {
                long costTime = System.currentTimeMillis() - startTime;
                auditLog.setCostTime(costTime);

                // 组装详细上下文信息 (HTTP & User)
                finishLogAssembly(point, auditLog);

                // 核心：异步投递至 RocketMQ
                // 实现了业务主路径的 "Fire-and-Forget" (发送即忘)，最大化接口响应速度
                logDispatchService.dispatch(auditLog);
            } catch (Exception e) {
                // 兜底保护：切面内的非核心逻辑错误不应阻断主业务流程
                log.error("[Audit Log] 审计日志分发异常", e);
            }
        }
        return result;
    }

    /**
     * 组装完整的日志上下文信息
     */
    private void finishLogAssembly(ProceedingJoinPoint point, AuditLog auditLog) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();

            // 1. 注入身份上下文 (User Identity)
            Long userId = securityUtils.getUserId();
            String userName = securityUtils.getUsername();

            if (userId == null) {
                Object tempId = request.getAttribute(LogConstants.LOG_ATTR_USER_ID);
                Object tempName = request.getAttribute(LogConstants.LOG_ATTR_USER_NAME);
                if (tempId != null) {
                    userId = Convert.toLong(tempId);
                    userName = String.valueOf(tempName);
                } else {
                    userId = -1L;
                    userName = "Anonymous";
                }
            }

            auditLog.setUserId(userId).setUserName(userName);

            // 2. 注入环境上下文 (Environment)
            auditLog.setIp(JakartaServletUtil.getClientIP(request)) // 使用 Hutool 工具类处理 X-Forwarded-For
                    .setRequestMethod(request.getMethod())
                    .setRequestUrl(request.getRequestURI());

            // 3. 注入数据上下文 (Data Payload)
            // 截取前 20000 字符，防止恶意大包导致消息中间件拒收
            auditLog.setParams(StrUtil.sub(argsToJson(point.getArgs()), 0, 20000));

        } catch (Exception e) {
            log.warn("[Audit Log] 请求上下文组装失败", e);
        }
    }

    /**
     * 参数序列化工具
     * <p>
     * <b>安全策略：</b>
     * 自动剔除无法序列化的系统对象 (Request, Response, InputStream 等)，防止 Jackson 报错。
     */
    private String argsToJson(Object obj) {
        if (obj == null) return null;
        try {
            List<Object> args = Arrays.stream((Object[]) obj)
                    .filter(arg -> !(arg instanceof HttpServletRequest)
                            && !(arg instanceof HttpServletResponse)
                            && !(arg instanceof MultipartFile)
                            && !(arg instanceof BindingResult))
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            // 序列化失败时仅记录简要错误，避免抛出异常影响主流程
            return "{\"msg\": \"参数序列化失败 (Serialization Failed)\"}";
        }
    }
}