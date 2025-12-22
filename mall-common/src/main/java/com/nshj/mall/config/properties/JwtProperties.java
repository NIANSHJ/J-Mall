package com.nshj.mall.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 令牌配置属性类
 * <p>
 * 基于 Spring Boot 配置属性绑定机制，自动映射配置文件中以 {@code mall.jwt} 为前缀的参数。
 * 集中管理 JSON Web Token 生成与验证过程中所需的核心策略参数，包括加密密钥、生命周期及协议规范等。
 * 支持多环境配置隔离，确保生产环境的安全敏感参数能够通过外部化配置进行管理。
 *
 * @author nshj
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥 (Secret Key)
     * <p>
     * 用于对 Token 进行 HMAC 加密签名的核心私钥凭证。
     * <p>
     * <b>安全规范：</b>
     * <ul>
     * <li>开发环境可配置简单字符串便于调试。</li>
     * <li>生产环境必须配置高强度的随机字符串（建议 32 字符以上），并建议通过环境变量或密钥管理系统注入，严禁硬编码。</li>
     * </ul>
     */
    private String secret;

    /**
     * 令牌有效时长
     * <p>
     * 定义 Access Token 从签发时刻起的存活周期。
     * <p>
     * <b>配置建议：</b>
     * 这里的数值单位通常取决于业务实现（通常为毫秒）。
     * 时间设置应在“用户免登体验”与“密钥泄露风险”之间取得平衡。
     */
    private Long expiration;

    /**
     * 令牌签发者标识 (Issuer)
     * <p>
     * 对应 JWT Payload 标准声明中的 {@code iss} 字段。
     * 在 Token 校验阶段，用于验证令牌来源的合法性，防止非授权服务签发的令牌被系统接受。
     */
    private String issuer;

    /**
     * HTTP 认证头前缀
     * <p>
     * 定义客户端在 HTTP 请求头 {@code Authorization} 中携带 Token 时的标准前缀标识。
     * <p>
     * <b>示例：</b>
     * 若配置为 "Bearer"，则请求头格式应为：{@code Authorization: Bearer <token_string>}。
     * 符合 OAuth 2.0 及 RFC 6750 标准规范。
     */
    private String prefix;
}