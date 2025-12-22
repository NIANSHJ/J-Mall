package com.nshj.mall.exception;

import com.nshj.mall.response.ReturnCode;
import lombok.Getter;

/**
 * 全局通用业务异常封装类
 * <p>
 * 专用于处理业务逻辑层面的预期性错误（如参数校验失败、库存不足、权限受限等）。
 * 该类继承自 {@link RuntimeException}，旨在利用 Java 的非受检异常机制实现以下架构目标：
 * <ul>
 * <li><b>事务原子性：</b> 抛出此异常时将自动触发 Spring {@code @Transactional} 的回滚策略，保障数据一致性。</li>
 * <li><b>统一响应：</b> 配合全局异常处理器 (GlobalExceptionHandler)，将异常堆栈转换为标准化的 JSON 响应结构返回给前端。</li>
 * </ul>
 *
 * @author nshj
 * @version 1.0.0
 * @see RuntimeException
 * @see com.nshj.mall.response.ReturnCode
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 业务响应状态码
     * <p>
     * 用于细粒度标识具体的业务错误类型。
     * 与 HTTP 状态码（200, 404, 500）不同，该状态码通常由业务字典定义，
     * 供前端用于区分错误场景并执行相应的交互逻辑（如：Code=401 时跳转登录页）。
     */
    private final int code;

    /**
     * 基于默认状态码构建业务异常
     * <p>
     * 适用于通用的服务端错误场景，仅需返回具体的错误描述信息。
     * 默认采用 {@link ReturnCode#RC500} 作为状态码。
     *
     * @param message 具体的异常描述文本
     */
    public BusinessException(String message) {
        super(message);
        this.code = ReturnCode.RC500.getCode();
    }

    /**
     * 构建自定义状态码与消息的业务异常
     * <p>
     * 允许在枚举定义之外，灵活指定特定的错误码与错误信息。
     *
     * @param code    自定义的业务错误码
     * @param message 具体的异常描述文本
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 基于标准枚举构建业务异常 (推荐模式)
     * <p>
     * 直接使用 {@link ReturnCode} 枚举抛出异常，这是规范化开发的最佳实践。
     * 能够确保系统中所有的错误定义（Code 和 Message）保持统一，避免硬编码字符串。
     *
     * @param returnCode 包含错误码与描述信息的标准结果枚举
     */
    public BusinessException(ReturnCode returnCode) {
        super(returnCode.getMessage());
        this.code = returnCode.getCode();
    }
}