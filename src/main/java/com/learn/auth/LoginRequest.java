package com.learn.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录接口接收的 JSON 凭证请求体，包含用于定位账号的用户名和待验证的原始密码。
 * 控制器通过 {@code @Valid} 触发 Jakarta Validation；任一字段为 {@code null}、空串或仅含空白字符时，
 * 请求都会在进入登录认证逻辑前校验失败。
 *
 * @param name 用于按原值查询用户记录的用户名；至少需要包含一个非空白字符
 * @param pw 客户端提交的原始密码，仅用于通过 {@code PasswordEncoder.matches} 与存储的 BCrypt 哈希比对；不得记录到日志或持久化
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String name,
        @NotBlank(message = "密码不能为空") String pw) {
}