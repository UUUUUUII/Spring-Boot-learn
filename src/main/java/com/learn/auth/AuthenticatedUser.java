package com.learn.auth;

/**
 * 登录验证通过后代表当前用户的最小身份对象。
 *
 * <p>它只包含 JWT 声明和请求身份所需的信息，不包含密码。</p>
 *
 * @param id 用户主键，用作 JWT 的 subject
 * @param name 用户名，用于请求处理期间识别用户
 * @param role 用户角色，可供后续授权逻辑判断
 */
public record AuthenticatedUser(String id, String name, String role) {
}