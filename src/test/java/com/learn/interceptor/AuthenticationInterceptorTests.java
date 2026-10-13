package com.learn.interceptor;

import com.learn.auth.AuthenticatedUser;
import com.learn.auth.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 覆盖身份拦截器的凭证选择、Session CSRF 校验和拒绝状态码。 */
class AuthenticationInterceptorTests {

    /** 测试专用 Base64 密钥；只用于单元测试，不得用于实际部署。 */
    private static final String JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private AuthenticatedUser user;
    private JwtTokenService jwtTokenService;
    private AuthenticationInterceptor interceptor;

    /** 每个测试使用相同的模拟用户和新建拦截器，避免状态互相影响。 */
    @BeforeEach
    void setUp() {
        user = new AuthenticatedUser("user-1", "alice", "user");
        jwtTokenService = new JwtTokenService(JWT_SECRET, 900);
        interceptor = new AuthenticationInterceptor(jwtTokenService, JsonMapper.builder().build());
    }

    /** 有效签名的 Bearer JWT 应通过，并将令牌身份放入当前请求属性。 */
    @Test
    void allowsValidBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtTokenService.issue(user).value());

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(user, request.getAttribute(AuthenticationInterceptor.REQUEST_USER_ATTRIBUTE));
    }

    /** 已存在的登录 Session 可以访问安全读取请求。 */
    @Test
    void allowsAuthenticatedSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.getSession().setAttribute(AuthenticationInterceptor.SESSION_USER_ATTRIBUTE, user);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertSame(user, request.getAttribute(AuthenticationInterceptor.REQUEST_USER_ATTRIBUTE));
    }

    /** 创建用户接口公开，但同路径的读取请求仍要求身份认证。 */
    @Test
    void onlyAllowsUnauthenticatedPostToUsersCollection() throws Exception {
        MockHttpServletRequest postRequest = new MockHttpServletRequest("POST", "/api/users");
        MockHttpServletRequest getRequest = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse getResponse = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(postRequest, new MockHttpServletResponse(), new Object()));
        assertFalse(interceptor.preHandle(getRequest, getResponse, new Object()));
        assertEquals(401, getResponse.getStatus());
    }

    /** Session 自动随浏览器发送，因此写请求没有 CSRF token 时应返回 403。 */
    @Test
    void rejectsSessionWriteWithoutCsrfToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/user-1");
        request.getSession().setAttribute(AuthenticationInterceptor.SESSION_USER_ATTRIBUTE, user);
        request.getSession().setAttribute(AuthenticationInterceptor.SESSION_CSRF_ATTRIBUTE, "expected-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    /** Session 写请求带回匹配的 CSRF token 后才放行。 */
    @Test
    void allowsSessionWriteWithCsrfToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/user-1");
        request.getSession().setAttribute(AuthenticationInterceptor.SESSION_USER_ATTRIBUTE, user);
        request.getSession().setAttribute(AuthenticationInterceptor.SESSION_CSRF_ATTRIBUTE, "expected-token");
        request.addHeader("X-CSRF-TOKEN", "expected-token");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    /** 未提供任何凭证时应返回 401 并阻止控制器执行。 */
    @Test
    void rejectsRequestWithoutCredentials() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, new Object()));
        assertEquals(401, response.getStatus());
    }

    /** 显式提供无效 Bearer 时不能借同一请求携带的 Session 作为回退凭证。 */
    @Test
    void rejectsInvalidBearerInsteadOfFallingBackToSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        request.getSession().setAttribute(AuthenticationInterceptor.SESSION_USER_ATTRIBUTE, user);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }
}