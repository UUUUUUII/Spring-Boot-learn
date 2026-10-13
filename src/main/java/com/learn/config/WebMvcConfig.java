package com.learn.config;

import com.learn.interceptor.AuthenticationInterceptor;
import com.learn.interceptor.RequestLoggingInterceptor;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类，集中注册应用使用的拦截器。
 *
 * <p>拦截器类只定义请求处理逻辑；在这里配置它何时、对哪些请求生效。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 记录所有请求处理结果的日志拦截器。 */
    private final RequestLoggingInterceptor requestLoggingInterceptor;
    /** 只为受保护 API 校验身份的拦截器。 */
    private final AuthenticationInterceptor authenticationInterceptor;

    /**
     * 使用构造器注入拦截器，确保配置类创建时依赖已准备好。
     *
    * @param requestLoggingInterceptor 负责记录所有匹配请求的响应状态和处理耗时
    * @param authenticationInterceptor 负责校验受保护 API 的 JWT 或 Session 身份
     */
    public WebMvcConfig(RequestLoggingInterceptor requestLoggingInterceptor,
                        AuthenticationInterceptor authenticationInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
        this.authenticationInterceptor = authenticationInterceptor;
    }

    /**
     * 将请求日志和身份认证拦截器注册到 Spring MVC，并分别设置其生效路径。
     * 请求日志拦截器匹配所有应用路径；身份认证拦截器只匹配 {@code /api/**}，
     * 公开登录接口和公开创建用户接口通过排除规则跳过身份校验。
     *
     * @param registry Spring MVC 提供的拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**");
        // 登录和用户创建接口允许匿名访问；其余 /api/** 请求必须先通过身份验证。
        registry.addInterceptor(authenticationInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(List.of("/api/auth/login", "/api/users/create"));
    }
}