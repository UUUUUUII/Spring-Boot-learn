package com.learn.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.domain.Users;
import com.learn.mapper.UsersMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从数据库加载登录凭证，供 {@code DaoAuthenticationProvider} 使用。
 * Spring Security 会在认证时调用此服务，再用配置的 PasswordEncoder 比对密码。
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UsersMapper usersMapper;

    public DatabaseUserDetailsService(UsersMapper usersMapper) {
        this.usersMapper = usersMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 用户名是登录标识；LambdaQueryWrapper 保留了实体字段的编译期类型检查。
        Users user = usersMapper.selectOne(new LambdaQueryWrapper<Users>()
                .eq(Users::getName, username));
        if (user == null) {
            // UserDetailsService 约定找不到用户时抛异常，不能返回 null。
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        // 分离业务身份和密码哈希，确保密码不会进入 JWT。
        AuthenticatedUser identity = new AuthenticatedUser(user.getId(), user.getName(), user.getRole());
        return new AuthenticatedUserDetails(identity, user.getPw());
    }
}