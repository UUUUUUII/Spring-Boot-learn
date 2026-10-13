package com.learn.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * 在 Spring Security 过滤链中验证 Bearer JWT。
 *
 * <p>验签和过期时间检查由 {@link JwtTokenService#parse(String)} 完成；通过后将身份写入
 * {@code SecurityContextHolder}，后续授权规则便可把请求视为已认证。</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtTokenService = jwtTokenService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 登录接口尚未拿到本次 JWT；即使客户端带着旧的无效 token，也允许重新登录。
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/login".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            // 缺少凭证时交给后面的 AuthorizationFilter；它会按路由规则决定是否返回 401。
            chain.doFilter(request, response);
            return;
        }
        if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            // 请求显式提供了其他认证方案时拒绝，不悄悄忽略错误凭证。
            reject(request, response);
            return;
        }

        try {
            // parse 会验签并校验 exp，不能只解码 JWT 的 claims 后信任其中内容。
            AuthenticatedUser user = jwtTokenService.parse(authorization.substring(7).trim());
            String role = user.role() == null || user.role().isBlank() ? "USER" : user.role();
            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase(Locale.ROOT);
            // principal 保存业务身份，authority 保存授权角色；密码不会从 JWT 恢复或放入上下文。
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    user, null, java.util.List.of(new SimpleGrantedAuthority(authority)));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException exception) {
            // 清除可能存在的上下文，之后通过统一入口点生成项目格式的 401 响应。
            SecurityContextHolder.clearContext();
            reject(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // 复用 Spring Security 的认证入口点，保证无凭证和无效凭证的响应格式一致。
        authenticationEntryPoint.commence(request, response,
                new org.springframework.security.authentication.BadCredentialsException("无效的 Bearer token"));
    }
}