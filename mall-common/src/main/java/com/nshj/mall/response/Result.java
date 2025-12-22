package com.nshj.mall.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一 API 响应结果封装体
 * <p>
 * 定义系统对外暴露的标准 HTTP 接口响应协议。
 * 旨在统一前后端交互的数据格式，确保所有业务场景（成功、失败、异常）均返回一致的 JSON 结构，
 * 便于前端框架进行统一的拦截、解析与错误处理。
 * <p>
 * <b>标准响应结构：</b>
 * <pre>
 * {
 * "code": 200,          // 业务状态码 (非 HTTP 状态码)
 * "msg": "操作成功",     // 提示消息
 * "data": { ... }       // 泛型数据载荷
 * }
 * </pre>
 *
 * @param <T> 响应数据载荷的泛型类型
 * @author nshj
 * @version 1.0.0
 * @see ReturnCode
 */
@Data
@Schema(description = "统一 API 响应协议")
public class Result<T> {

    /**
     * 业务状态码
     * <p>
     * <b>设计说明：</b>
     * 该字段用于标识业务层面的执行结果，<b>不同于</b> HTTP 协议状态码 (200, 404, 500)。
     * 前端应当优先判断 HTTP 状态码是否为 200，然后再解析此字段判断业务逻辑是否成功。
     * <ul>
     * <li><b>200:</b> 业务处理成功</li>
     * <li><b>非200:</b> 业务处理失败或异常，具体含义请参照 {@link ReturnCode}</li>
     * </ul>
     */
    @Schema(description = "业务状态码 (200=成功, 其他=失败)", example = "200")
    private int code;

    /**
     * 响应描述信息
     * <p>
     * 包含面向用户的提示文案（如“操作成功”），或面向开发者的调试错误信息（如“参数校验失败：缺少必填项”）。
     */
    @Schema(description = "结果描述信息", example = "操作成功")
    private String msg;

    /**
     * 业务数据载荷
     * <p>
     * 承载实际的业务 DTO、VO 对象或集合数据。
     * <br>当 {@code code != 200} 时，此字段通常为 {@code null}，但也允许返回部分错误详情数据。
     */
    @Schema(description = "业务数据载荷")
    private T data;

    /**
     * 构造成功响应（带数据）
     * <p>
     * 适用于查询请求、详情获取等需要向前端返回具体内容的场景。
     *
     * @param data 具体的业务数据对象
     * @param <T>  数据类型泛型
     * @return 状态码为 200 的成功响应实体
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ReturnCode.RC200.getCode());
        result.setMsg(ReturnCode.RC200.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 构造成功响应（无数据）
     * <p>
     * 适用于删除、更新、发送指令等仅需告知前端“操作成功”，而无需返回具体数据的场景。
     *
     * @param <T> 数据类型泛型
     * @return 状态码为 200 且 data 为 null 的成功响应实体
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 构造失败响应（自定义消息）
     * <p>
     * 允许在运行时动态指定错误码与描述信息。
     * 通常用于参数校验器（Validator）返回具体的字段错误信息，或处理非标准化的临时业务错误。
     *
     * @param code 自定义业务错误码
     * @param msg  自定义错误描述
     * @param <T>  数据类型泛型
     * @return 包含错误详情的失败响应实体
     */
    public static <T> Result<T> fail(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    /**
     * 构造失败响应（基于标准枚举）
     * <p>
     * <b>最佳实践：</b> 推荐使用此方法构建错误响应。
     * 强制要求使用 {@link ReturnCode} 枚举，确保全系统错误定义的统一性与可维护性，
     * 避免代码中出现难以维护的“魔法数字” (Magic Number)。
     *
     * @param returnCode 包含标准错误码和描述的枚举对象
     * @param <T>        数据类型泛型
     * @return 标准化的失败响应实体
     */
    public static <T> Result<T> fail(ReturnCode returnCode) {
        Result<T> result = new Result<>();
        result.setCode(returnCode.getCode());
        result.setMsg(returnCode.getMessage());
        return result;
    }
}