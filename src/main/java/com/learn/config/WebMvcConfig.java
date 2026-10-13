package com.learn.config;

import com.learn.interceptor.RequestLoggingInterceptor;

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
     /**
      * 使用构造器注入日志拦截器。
     *
      * @param requestLoggingInterceptor 负责记录所有匹配请求的响应状态和处理耗时
     */
     public WebMvcConfig(RequestLoggingInterceptor requestLoggingInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    /**
        * 注册请求日志拦截器；API 身份认证由 Spring Security Filter Chain 负责。
     *
     * @param registry Spring MVC 提供的拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**");
    }
}