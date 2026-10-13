package com.learn.config;

import com.learn.auth.JwtAuthenticationFilter;
import com.learn.auth.JwtTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.ProviderManager;
import tools.jackson.databind.ObjectMapper;

/** 使用 Spring Security Filter Chain 统一处理登录认证和 API 授权。 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 组合数据库用户服务和 BCrypt 编码器，供登录控制器调用。
     * DaoAuthenticationProvider 负责加载用户并通过 PasswordEncoder.matches 校验密码。
     */
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /** 未认证或 JWT 无效时返回项目统一格式的 HTTP 401。 */
    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> SecurityErrorResponse.write(
                objectMapper, response, HttpServletResponse.SC_UNAUTHORIZED, "需要有效身份认证");
    }

    /** 身份有效但没有所需权限时返回项目统一格式的 HTTP 403。 */
    @Bean
    AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> SecurityErrorResponse.write(
                objectMapper, response, HttpServletResponse.SC_FORBIDDEN, "无权访问该资源");
    }

    /**
     * 构建无状态 API 安全链：公开登录和注册，其余 API 必须先通过 JWT Filter。
     * 表单登录、Basic 和服务端 Session 都关闭，避免出现第二套凭证机制。
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuthenticationManager authenticationManager,
                                            AuthenticationEntryPoint authenticationEntryPoint,
                                            AccessDeniedHandler accessDeniedHandler,
                                            JwtTokenService jwtTokenService) {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenService, authenticationEntryPoint);
        http.authenticationManager(authenticationManager)
                // 凭证只从 Authorization: Bearer 读取，不使用自动附带的 Cookie，因此无需 CSRF token。
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                    // 注册与登录允许匿名访问；其余 API 要求上下文中已有认证身份。
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users/create").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                // 在用户名密码过滤器之前解析 JWT，让后续授权过滤器能读取认证身份。
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}