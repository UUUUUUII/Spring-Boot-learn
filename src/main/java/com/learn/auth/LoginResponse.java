package com.learn.auth;

import java.time.Instant;

/**
 * 登录成功后返回给客户端的凭证和有效期信息。
 *
 * <p>客户端可在后续请求中选择 JWT 或登录时创建的 Session 进行身份认证。Session ID 由 Servlet
 * 容器通过 {@code JSESSIONID} Cookie 设置；{@code csrfToken} 与该 Session 绑定，只在使用 Session
 * 调用写接口时需要通过 {@code X-CSRF-TOKEN} 请求头回传。</p>
 *
 * @param accessToken 已签名的 JWT，客户端应通过 {@code Authorization: Bearer ...} 请求头发送
 * @param tokenType JWT 的认证方案标识，当前固定为 {@code Bearer}
 * @param expiresAt JWT 的失效时间；过期后客户端需要重新登录获取新令牌
 * @param csrfToken 当前 Session 对应的防伪 token；使用 Bearer JWT 时不需要发送该值
 */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, String csrfToken) {
}