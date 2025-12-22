package com.nshj.mall.exception;

import com.nshj.mall.response.Result;
import com.nshj.mall.response.ReturnCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常统一处理切面类
 * <p>
 * 基于 {@link RestControllerAdvice} 机制，对 Controller 层抛出的各类异常进行集中捕获与处理。
 * 旨在将后端复杂的 Java 异常堆栈转换为前端可读的标准化 JSON 响应结构 ({@link Result})，
 * 并根据异常的严重程度实施差异化的日志记录与安全信息屏蔽策略。
 *
 * @author nshj
 * @version 1.0.0
 * @see org.springframework.web.bind.annotation.RestControllerAdvice
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 拦截处理自定义业务异常
     * <p>
     * <b>触发场景：</b>
     * 业务逻辑层 (Service) 在处理过程中遇到预期内的阻断性错误（如：库存不足、账号冻结、权限校验失败）时主动抛出的异常。
     * <p>
     * <b>处理策略：</b>
     * <ul>
     * <li><b>日志记录：</b> 级别为 WARN。此类异常属于业务流程分支，非系统故障，无需打印堆栈追踪。</li>
     * <li><b>响应封装：</b> 直接透传异常对象中携带的业务状态码 (Code) 与提示信息 (Message) 给前端。</li>
     * </ul>
     *
     * @param e 捕获到的业务异常对象
     * @return 包含具体业务错误详情的标准化响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务阻断: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 拦截处理参数校验异常
     * <p>
     * <b>触发场景：</b>
     * 前端提交的请求参数未通过 DTO 对象上的 JSR-303 注解校验（如 {@code @NotNull}, {@code @Size}）时，
     * Spring MVC 框架会自动抛出 {@link MethodArgumentNotValidException}。
     * <p>
     * <b>处理策略：</b>
     * <ul>
     * <li><b>错误提取：</b> 解析 {@link BindingResult}，仅提取<b>第一条</b>字段错误提示，避免一次性暴露过多校验细节。</li>
     * <li><b>响应封装：</b> 统一映射为 {@link ReturnCode#RC400} (请求参数错误)，并将字段错误信息作为提示返回。</li>
     * </ul>
     *
     * @param e 参数校验异常上下文对象
     * @return 状态码 400 及具体字段校验失败信息的响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMsg = "请求参数格式错误";

        // 提取第一条错误详情，避免将所有错误堆叠给前端
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                errorMsg = fieldError.getDefaultMessage();
            }
        }

        log.warn("参数校验未通过: {}", errorMsg);
        return Result.fail(ReturnCode.RC400.getCode(), errorMsg);
    }

    /**
     * 拦截处理全局未知系统异常 (兜底策略)
     * <p>
     * <b>触发场景：</b>
     * 代码运行中发生的空指针 (NPE)、数据库连接超时、SQL 语法错误等所有未被单独捕获的非预期故障。
     * <p>
     * <b>安全策略 (Security Masking)：</b>
     * <ul>
     * <li><b>日志记录：</b> <font color="red">级别为 ERROR</font>。必须记录完整的堆栈追踪 (Stack Trace) 以便运维排查 Bug。</li>
     * <li><b>响应封装：</b> <b>严禁</b>向前端返回具体的异常堆栈或 SQL 错误信息。统一返回模糊的 "系统异常" (500) 提示，
     * 防止因泄露代码路径、数据库表结构等敏感信息而导致的安全风险。</li>
     * </ul>
     *
     * @param e 捕获到的未知异常对象
     * @return 状态码 500 的通用错误响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统发生未知异常", e);
        return Result.fail(ReturnCode.RC500);
    }
}