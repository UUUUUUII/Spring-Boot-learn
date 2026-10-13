package com.learn.auth;

import java.io.Serializable;

/**
 * 登录验证通过后代表当前用户的最小身份对象。
 *
 * <p>它会保存在 HTTP Session 中，也会作为 JWT 声明的数据来源；刻意不包含密码，
 * 因而不会把密码写入 Session、请求属性或令牌。实现 {@link Serializable} 便于使用
 * 支持序列化的 Session 存储方式。</p>
 *
 * @param id 用户主键，用作 JWT 的 subject
 * @param name 用户名，用于请求处理期间识别用户
 * @param role 用户角色，可供后续授权逻辑判断
 */
public record AuthenticatedUser(String id, String name, String role) implements Serializable {
}