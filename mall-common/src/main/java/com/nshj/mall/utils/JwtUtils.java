package com.nshj.mall.utils;

import com.nshj.mall.config.properties.JwtProperties;
import com.nshj.mall.constant.JwtConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 令牌全生命周期管理组件
 * <p>
 * 基于 JJWT (Java JWT) 库构建，提供符合 RFC 7519 标准的 JSON Web Token 服务。
 * 负责系统核心的凭证颁发 (Sign)、合法性校验 (Verify) 与数据解析 (Extract)。
 * <p>
 * <b>核心架构职责：</b>
 * <ul>
 * <li><b>加密封装：</b> 屏蔽底层的 HMAC-SHA 签名算法细节，确保密钥安全。</li>
 * <li><b>载荷管理：</b> 统一管理 Payload 中的标准声明 (Registered Claims) 与私有字段。</li>
 * <li><b>类型安全：</b> 解决跨语言/跨库反序列化时，数值类型 (Integer vs Long) 的兼容性问题。</li>
 * </ul>
 *
 * @author nshj
 * @version 1.0.0
 * @see <a href="https://jwt.io/">JWT Debugger</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;

    /**
     * 生成 HMAC-SHA 数字签名密钥
     * <p>
     * 将配置文件中的明文字符串密钥转换为 Java Cryptography Architecture (JCA) 标准的 {@link SecretKey} 对象。
     * <br>
     * <b>编码规范：</b>
     * 强制使用 {@code UTF-8} 编码读取字节数组。防止在不同操作系统（如 Windows 默认 GBK，Linux 默认 UTF-8）下，
     * 因默认字符集不一致导致生成的密钥字节流不同，进而引发跨环境验签失败的问题。
     *
     * @return 适用于 HS256/HS384/HS512 算法的密钥实例
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发用户身份令牌 (Issue Token)
     * <p>
     * 构建并签名一个包含标准声明 (Registered Claims) 与业务声明 (Private Claims) 的 JWS 字符串。
     *
     * @param userId    用户主键 ID (用于后端数据索引，不建议放入过多敏感信息)
     * @param username  用户登录名 (作为 Subject)
     * @param tokenUuid 会话唯一指纹 (JTI)，用于关联 Redis 中的登录态，支持服务端主动踢出功能
     * @return Base64Url 编码的紧凑型 JWT 字符串
     */
    public String createToken(Long userId, String username, String tokenUuid) {
        // 1. 准备私有业务载荷
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put(JwtConstants.CLAIM_KEY_UUID, tokenUuid);

        // 2. 计算生命周期窗口
        long nowMillis = System.currentTimeMillis();
        Date issuedAt = new Date(nowMillis);
        Date expiration = new Date(nowMillis + jwtProperties.getExpiration());

        // 3. 构建并签名
        return Jwts.builder()
                .header().add("typ", "JWT").and()   // Header: 显式声明类型
                .claims(claims)                     // Payload: 注入自定义业务数据
                .issuer(jwtProperties.getIssuer())  // Payload: iss (签发者校验)
                .issuedAt(issuedAt)                 // Payload: iat (签发时间)
                .expiration(expiration)             // Payload: exp (过期时间)
                .subject(username)                  // Payload: sub (主题/用户名)
                .signWith(getSecretKey())           // Signature: 使用 HMAC 算法签名
                .compact();
    }

    /**
     * 解析并验证令牌 (Parse & Verify)
     * <p>
     * <b>验证流程：</b>
     * <ol>
     * <li><b>格式验证：</b> 检查是否为标准的三段式结构 (Header.Payload.Signature)。</li>
     * <li><b>签名验证：</b> 使用服务端私钥重新计算签名，确保 Payload 未被篡改。</li>
     * <li><b>时效验证：</b> 自动检查 {@code exp} 字段，若当前时间已超过有效期则抛出异常。</li>
     * </ol>
     *
     * @param token 待验证的 JWT 字符串 (不含 Bearer 前缀)
     * @return 解析后的载荷对象 (Claims)
     * @throws JwtException 当遇到签名无效、令牌过期或格式错误时抛出运行时异常，需调用方捕获处理
     */
    public Claims parseToken(String token) {
        JwtParser parser = Jwts.parser()
                .verifyWith(getSecretKey()) // 注入验签密钥
                .build();

        return parser.parseSignedClaims(token).getPayload();
    }

    /**
     * 安全提取用户 ID
     * <p>
     * 从 Token 中解析 {@code userId} 字段，通常用于 Filter 过滤器层快速识别用户身份。
     * <p>
     * <b>鲁棒性设计 (Robustness)：</b>
     * JSON 标准中数字均为 Number 类型。在反序列化时，较小的数值可能被转为 {@code Integer}，较大的转为 {@code Long}。
     * 若直接强转 {@code (Long)} 可能导致 {@link ClassCastException}。
     * <br>此处采用 {@code String -> Long} 的中转策略，确保无论底层解析为何种数字类型，都能安全转换为 Long。
     *
     * @param token JWT 字符串
     * @return 用户 ID (若解析失败或 Token 无效，返回 null 而不抛出异常，由调用方判定为未登录)
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            Object userId = claims.get("userId");
            // 防御性转换：Object -> String -> Long，兼容 Jackson 的动态数字处理策略
            return userId != null ? Long.valueOf(String.valueOf(userId)) : null;
        } catch (Exception e) {
            log.warn("JWT UserId 解析失败: 令牌无效或格式错误 - {}", e.getMessage());
            return null;
        }
    }

    /**
     * 提取会话指纹 (UUID)
     * <p>
     * 解析 {@code token_uuid} 字段。
     * 该字段是连接 "无状态 JWT" 与 "有状态 Redis" 的关键桥梁，用于校验 Token 是否在黑名单/白名单中。
     *
     * @param token JWT 字符串
     * @return 会话 UUID (解析失败返回 null)
     */
    public String getUuidFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return (String) claims.get(JwtConstants.CLAIM_KEY_UUID);
        } catch (Exception e) {
            return null;
        }
    }
}