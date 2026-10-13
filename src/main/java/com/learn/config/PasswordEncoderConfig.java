package com.learn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 注册密码编码器 Bean，供用户写入和登录校验共享。
 * BCrypt 会为每个密码生成随机盐；校验时使用 {@link PasswordEncoder#matches}，
 * 不应自行比较原始密码和数据库哈希字符串。
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 创建 BCrypt 实现供 Spring 注入。
     *
     * @return 用于单向哈希和密码匹配的编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}