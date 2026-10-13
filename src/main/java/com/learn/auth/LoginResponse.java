package com.learn.auth;

import java.time.Instant;

/**
 * 登录成功后返回给客户端的凭证和有效期信息。
 *
 * <p>客户端应在后续请求中通过 {@code Authorization: Bearer ...} 请求头发送 JWT。</p>
 *
 * @param accessToken 已签名的 JWT，客户端应通过 {@code Authorization: Bearer ...} 请求头发送
 * @param tokenType JWT 的认证方案标识，当前固定为 {@code Bearer}
 * @param expiresAt JWT 的失效时间；过期后客户端需要重新登录获取新令牌
 */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt) {
}