package com.learn.interceptor;

import com.learn.auth.AuthenticatedUser;
import com.learn.auth.JwtTokenService;
import com.learn.common.ApiResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 校验受保护 API 的身份，支持 Bearer JWT 和登录后建立的 HTTP Session。
 *
 * <p>显式提供 Authorization 头时只尝试验 JWT，失败即拒绝，不回退到 Cookie Session；
 * 没有 Authorization 头时才查找现有 Session。Session 写请求额外校验 CSRF token。</p>
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    /** 已认证身份在服务端 Session 中的属性名。 */
    public static final String SESSION_USER_ATTRIBUTE = AuthenticationInterceptor.class.getName() + ".USER";
    /** 当前请求上暴露身份信息的属性名，供后续控制器或授权逻辑读取。 */
    public static final String REQUEST_USER_ATTRIBUTE = AuthenticationInterceptor.class.getName() + ".USER";
    /** 当前 Session 对应的 CSRF token 属性名。 */
    public static final String SESSION_CSRF_ATTRIBUTE = AuthenticationInterceptor.class.getName() + ".CSRF";

    /** 负责对 JWT 签名和有效期进行验证的服务。 */
    private final JwtTokenService jwtTokenService;
    /** 用于将认证失败写成项目统一的 JSON 响应结构。 */
    private final ObjectMapper objectMapper;

    /**
     * 注入令牌解析器和 JSON 序列化器。
     *
     * @param jwtTokenService JWT 校验服务
     * @param objectMapper Spring 管理的 Jackson 序列化器
     */
    public AuthenticationInterceptor(JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * 在控制器执行前解析请求凭证；验证成功时把身份放入 request attribute 并继续处理。
     *
     * @param request 当前请求，凭证来自 Authorization 头或已有 Session
     * @param response 当前响应，认证失败时直接写入 401/403 JSON
     * @param handler Spring MVC 已解析出的处理器
     * @return 身份有效且 CSRF 检查通过时返回 true，否则终止请求并返回 false
     * @throws IOException 写入认证失败响应时发生的 I/O 错误
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        AuthenticatedUser user;
        String authorization = request.getHeader("Authorization");

        if (authorization != null) {
            // 请求明确选择了 Authorization 凭证；未知认证方案或空 Bearer token 都拒绝。
            if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return reject(response);
            }
            String token = authorization.substring(7).trim();
            if (token.isEmpty()) {
                return reject(response);
            }
            try {
                // parse 会验签并检查 exp，不能仅对 JWT 做 Base64 解码后相信其声明。
                user = jwtTokenService.parse(token);
            } catch (JwtException | IllegalArgumentException exception) {
                // 不向客户端暴露签名细节，也不尝试退回到可能同时存在的 Session。
                return reject(response);
            }
        } else {
            // 不创建新 Session；没有已经登录的 Session 时直接视为未认证。
            HttpSession session = request.getSession(false);
            Object sessionUser = session == null ? null : session.getAttribute(SESSION_USER_ATTRIBUTE);
            if (!(sessionUser instanceof AuthenticatedUser authenticatedUser)) {
                return reject(response);
            }
            user = authenticatedUser;
            // 浏览器会自动附带 Cookie，因此 Session 的写请求必须显式证明请求来自合法页面。
            if (requiresCsrfProtection(request.getMethod()) && !hasValidCsrfToken(request, session)) {
                return reject(response, HttpServletResponse.SC_FORBIDDEN, "CSRF 校验失败");
            }
        }

        // 将已验证身份交给当前请求后续处理，不写入拦截器实例字段以免并发请求串用户。
        request.setAttribute(REQUEST_USER_ATTRIBUTE, user);
        return true;
    }

    /** 未提供或无效认证凭证时使用的默认 401 响应。 */
    private boolean reject(HttpServletResponse response) throws IOException {
        return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "需要有效身份认证");
    }

    /**
     * 写出统一格式的认证错误响应，并阻止 DispatcherServlet 继续调用控制器。
     *
     * @param response 当前 HTTP 响应
     * @param status HTTP 状态码，认证失败为 401，CSRF 失败为 403
     * @param message 返回给客户端的简短错误信息
     * @return 始终返回 false，表示拦截当前请求
     */
    private boolean reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(message));
        return false;
    }

    /**
     * 将请求头中的 CSRF token 与当前 Session 保存值进行常量时间比较。
     *
     * @param request 当前写请求
     * @param session 已通过身份校验的 Session
     * @return 两边 token 均存在且内容相同时返回 true
     */
    private boolean hasValidCsrfToken(HttpServletRequest request, HttpSession session) {
        Object expected = session.getAttribute(SESSION_CSRF_ATTRIBUTE);
        String provided = request.getHeader("X-CSRF-TOKEN");
        if (!(expected instanceof String expectedToken) || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 判断请求方法是否会改变服务端状态；安全读取方法不要求 CSRF token。
     *
     * @param method HTTP 方法名
     * @return 非 GET、HEAD、OPTIONS、TRACE 方法返回 true
     */
    private boolean requiresCsrfProtection(String method) {
        return !"GET".equalsIgnoreCase(method)
                && !"HEAD".equalsIgnoreCase(method)
                && !"OPTIONS".equalsIgnoreCase(method)
                && !"TRACE".equalsIgnoreCase(method);
    }
}