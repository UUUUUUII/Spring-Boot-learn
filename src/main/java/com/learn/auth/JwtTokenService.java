package com.learn.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * 负责签发和验证 HMAC-SHA256 JWT。
 *
 * <p>签名密钥从 {@code app.security.jwt.secret} 读取，必须是足够长度的 Base64 密钥；
 * 令牌的签名校验和有效期检查都由 JJWT 完成，调用方不得只解码而不验证令牌。</p>
 */
@Component
public class JwtTokenService {

    /** JWT 签名及验签共用的 HMAC 密钥。 */
    private final SecretKey signingKey;
    /** 新签发令牌的有效时长。 */
    private final Duration tokenLifetime;

    /**
     * 从配置读取密钥与令牌有效期，并在启动时检查配置是否合法。
     *
     * @param base64Secret Base64 编码的 HMAC 密钥
     * @param expirationSeconds JWT 有效秒数，必须大于零
     */
    public JwtTokenService(
            @Value("${app.security.jwt.secret}") String base64Secret,
            @Value("${app.security.jwt.expiration-seconds:900}") long expirationSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT 过期时间必须大于 0 秒");
        }
        this.tokenLifetime = Duration.ofSeconds(expirationSeconds);
    }

    /**
     * 创建带用户身份声明和过期时间的签名令牌。
     *
     * @param user 已认证用户，不包含密码
     * @return JWT 字符串及其过期时间
     */
    public IssuedJwt issue(AuthenticatedUser user) {
        // Instant 使用 UTC 时间点；JWT 的 iat/exp 标准字段由 Date 表示。
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenLifetime);
        String token = Jwts.builder()
                .subject(user.id())
                .claim("name", user.name())
                .claim("role", user.role())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedJwt(token, expiresAt);
    }

    /**
     * 验证签名和有效期，并从可信的 JWT 声明中还原用户身份。
     * JJWT 会在签名错误、格式错误或令牌过期时抛出 JwtException；缺少业务身份声明时抛出参数异常。
     *
     * @param token 请求提供的 JWT 字符串，不应包含 {@code Bearer } 前缀
     * @return 从经过验签的 claims 中恢复出的用户身份
     */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String userId = claims.getSubject();
        String name = claims.get("name", String.class);
        String role = claims.get("role", String.class);
        if (userId == null || name == null || role == null) {
            throw new IllegalArgumentException("JWT 缺少用户身份信息");
        }
        return new AuthenticatedUser(userId, name, role);
    }

    /**
     * 签发令牌时一并返回的令牌文本和过期时间，避免调用方重新解析 JWT 获取过期时间。
     *
     * @param value 完整的签名 JWT 字符串，可作为 Bearer 凭证发送
     * @param expiresAt 该 JWT 的失效时间，与令牌中的 {@code exp} 声明一致
     */
    public record IssuedJwt(String value, Instant expiresAt) {
    }
}