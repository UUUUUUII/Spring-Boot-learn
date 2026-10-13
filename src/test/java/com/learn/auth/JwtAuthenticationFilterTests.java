package com.learn.auth;

import com.learn.config.SecurityErrorResponse;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 JWT 过滤器对有效、缺失和无效 Bearer 凭证的处理。 */
class JwtAuthenticationFilterTests {

    /** 测试专用 Base64 密钥，只用于本地单元测试。 */
    private static final String JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private JwtTokenService jwtTokenService;
    private JwtAuthenticationFilter filter;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(JWT_SECRET, 900);
        user = new AuthenticatedUser("user-1", "alice", "user");
        var objectMapper = JsonMapper.builder().build();
        AuthenticationEntryPoint entryPoint = (request, response, exception) ->
            SecurityErrorResponse.write(objectMapper, response, 401, "需要有效身份认证");
        filter = new JwtAuthenticationFilter(jwtTokenService, entryPoint);
    }

    @AfterEach
    void clearSecurityContext() {
        // SecurityContextHolder 使用线程本地状态；测试间清理可避免认证身份串到下一例。
        SecurityContextHolder.clearContext();
    }

    /** 正确签名的 JWT 应被转换成已认证身份和 ROLE_USER authority。 */
    @Test
    void authenticatesValidBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("Authorization", "Bearer " + jwtTokenService.issue(user).value());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();
        FilterChain chain = (servletRequest, servletResponse) -> continued.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(continued.get());
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(authentication.isAuthenticated());
        assertEquals(user, authentication.getPrincipal());
        assertEquals("ROLE_USER", authentication.getAuthorities().iterator().next().getAuthority());
    }

    /** 没有 Authorization 头时由过滤链继续处理，最终是否放行由授权规则决定。 */
    @Test
    void continuesWithoutCredentialsForSecurityAuthorizationToReject() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> continued.set(true));

        assertTrue(continued.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /** 无效 JWT 必须清空身份上下文并通过入口点返回 401，不能进入控制器。 */
    @Test
    void rejectsInvalidBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new AssertionError("Invalid token must stop the filter chain");
        });

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}