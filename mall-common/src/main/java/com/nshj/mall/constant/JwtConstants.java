package com.nshj.mall.constant;

/**
 * JWT 认证协议常量定义类
 * <p>
 * 集中管理系统身份认证流程中的标准化字符串常量，旨在消除硬编码（Magic String）。
 * 本类涵盖了标准 HTTP 认证头名称、Bearer 协议规范前缀以及 JWT 载荷（Payload）中的扩展键名定义，
 * 确保鉴权过滤器（Filter）与令牌工具类（Utils）之间的协议一致性。
 *
 * @author nshj
 * @version 1.0.0
 */
public class JwtConstants {

    /**
     * HTTP 认证请求头名称
     * <p>
     * 定义客户端在发起 API 请求时，传递身份凭证（Token）的标准 HTTP Header 字段。
     * 遵循 RFC 7235 HTTP 身份验证标准，服务端将通过此 Header 提取令牌。
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * 认证令牌协议前缀
     * <p>
     * 遵循 RFC 6750 标准定义的 Bearer 认证方案标识。
     * <p>
     * <b>开发注意：</b>
     * 字符串末尾包含一个强制的<b>空格 (Space)</b>。
     * 这种设计是为了在后端截取字符串时，能够直接分离协议头与真实的 Base64 令牌内容。
     * <br>完整格式示例：{@code Authorization: Bearer <Your_Token_String>}
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * JWT 载荷扩充字段：令牌唯一标识 (Token UUID)
     * <p>
     * 对应 JWT Claims 中的自定义 Key，用于存储该枚令牌的全局唯一 ID。
     * <p>
     * <b>架构设计用途：</b>
     * JWT 本质是无状态（Stateless）的，一旦签发无法撤销。
     * 通过解析此字段获取 UUID，并配合 Redis 实现“黑名单机制”或“白名单续期”，
     * 可有效解决 JWT 的<b>主动注销</b>、<b>单点登录互斥</b>及<b>防重放攻击</b>等安全难题。
     */
    public static final String CLAIM_KEY_UUID = "token_uuid";

    /**
     * 私有构造器
     * <p>
     * 这是一个纯静态常量容器类，明确禁止实例化。
     */
    private JwtConstants() {
    }
}