package com.learn.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Locale;

/**
 * Spring Security 登录阶段使用的用户对象。
 *
 * <p>{@link AuthenticatedUser} 用于签发 JWT，不包含密码；此类额外持有数据库中的 BCrypt 哈希，
 * 让 {@code DaoAuthenticationProvider} 能调用 {@code PasswordEncoder.matches} 校验密码。</p>
 */
public final class AuthenticatedUserDetails implements UserDetails {

    /** JWT 和后续业务识别需要的最小用户信息。 */
    private final AuthenticatedUser user;
    /** 数据库中保存的 BCrypt 哈希，不是客户端提交的明文密码。 */
    private final String password;
    /** Spring Security 用它执行 hasRole/hasAuthority 等授权判断。 */
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUserDetails(AuthenticatedUser user, String password) {
        this.user = user;
        this.password = password;
        String role = user.role() == null || user.role().isBlank() ? "USER" : user.role();
        // Spring Security 的 hasRole("USER") 实际匹配名为 ROLE_USER 的 authority。
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase(Locale.ROOT);
        this.authorities = java.util.List.of(new SimpleGrantedAuthority(authority));
    }

    /** 返回去掉密码后的业务身份，登录成功后用于写入 JWT。 */
    public AuthenticatedUser getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return user.name();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 当前认证流程只校验用户名和密码；如需禁用账号，应在这里映射 Users.active。
        return true;
    }
}