package com.nshj.mall.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 全局标准业务状态码枚举
 * <p>
 * 定义系统对外 API 接口的标准响应状态字典。
 * 本枚举配合 {@link Result} 类使用，构成了应用层的通信协议基础。
 * <p>
 * <b>设计哲学：</b>
 * <ul>
 * <li><b>语义对齐 (Semantic Alignment)：</b> 尽管这是业务逻辑层的状态码，但在数值定义上刻意与 HTTP 标准状态码（RFC 7231）保持一致（如 200, 401, 500）。
 * 这种设计降低了前后端协作的认知成本，使开发人员能直观理解错误类型。</li>
 * <li><b>信封模式 (Envelope Pattern)：</b> 即使业务执行失败，HTTP 传输层的状态码通常仍返回 200 OK，
 * 具体的业务结果（成功/失败/异常）由 Response Body 中的 {@code code} 字段决定。</li>
 * </ul>
 *
 * @author nshj
 * @version 1.0.0
 * @see Result
 */
@Getter
public enum ReturnCode {

    /**
     * 操作成功 (OK)
     * <p>
     * <b>含义：</b> 请求已被服务器正确接收、理解，并完成了预期的业务逻辑。
     * <br>对应 HTTP 状态码 200。
     */
    RC200(HttpStatus.OK.value(), "操作成功"),

    /**
     * 客户端请求错误 (Bad Request)
     * <p>
     * <b>含义：</b> 客户端发送的请求参数有误（如：必填项缺失、JSON 格式错误、参数类型不匹配）。
     * <br><b>触发场景：</b> 通常由 JSR-303/Bean Validation 参数校验框架抛出异常时触发。
     * <br><b>前端动作：</b> 提示用户检查输入内容。
     * <br>对应 HTTP 状态码 400。
     */
    RC400(HttpStatus.BAD_REQUEST.value(), "参数校验失败"),

    /**
     * 未经认证 / 凭证失效 (Unauthorized)
     * <p>
     * <b>含义：</b> 请求方身份未知，或携带的身份凭证（Token）无效/已过期。
     * <br><b>触发场景：</b> 请求头缺失 Authorization、JWT 签名校验失败、Token 超过有效期。
     * <br><b>前端动作：</b> <font color="red">拦截器捕获此码后，应强制清理本地缓存并跳转至登录页面。</font>
     * <br>对应 HTTP 状态码 401。
     */
    RC401(HttpStatus.UNAUTHORIZED.value(), "认证已失效，请重新登录"),

    /**
     * 访问权限受限 (Forbidden)
     * <p>
     * <b>含义：</b> 服务器已确认用户身份，但该用户无权访问请求的资源。
     * <br><b>触发场景：</b> 普通用户尝试调用管理员接口、或者访问未被授权的数据范围。
     * <br><b>前端动作：</b> 弹出“无权访问”提示或显示 403 缺省页，<b>不应</b>跳转登录页（因为重新登录也无法获取权限）。
     * <br>对应 HTTP 状态码 403。
     */
    RC403(HttpStatus.FORBIDDEN.value(), "无访问权限"),

    /**
     * 服务器内部错误 (Internal Server Error)
     * <p>
     * <b>含义：</b> 服务器处理请求时发生了未预期的错误。
     * <br><b>触发场景：</b> 空指针异常 (NPE)、数据库连接超时、第三方服务不可用等代码级 Bug。
     * <br><b>前端动作：</b> 提示“系统繁忙”，建议用户稍后重试。
     * <br>对应 HTTP 状态码 500。
     */
    RC500(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统繁忙，请稍后重试");

    /**
     * 业务状态码
     */
    private final int code;

    /**
     * 默认描述信息
     */
    private final String message;

    ReturnCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}