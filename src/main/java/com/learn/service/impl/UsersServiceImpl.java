package com.learn.service.impl;

import com.learn.domain.Users;
import com.learn.mapper.UsersMapper;
import com.learn.service.UsersService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserService 的默认实现。
 * 该层负责组织业务操作，并通过 Mapper 调用 MyBatis-Plus；不直接处理 HTTP 请求和响应。
 */
@Service
public class UsersServiceImpl implements UsersService {

    /** final 字段配合构造器注入，依赖创建后不可替换，也便于单元测试时提供替身实现。 */
    private final UsersMapper usersMapper;
    /** 负责对新密码做 BCrypt 哈希；数据库中不保存客户端提交的明文密码。 */
    private final PasswordEncoder passwordEncoder;

    /** Spring 自动注入数据访问对象和密码哈希器。 */
    public UsersServiceImpl(UsersMapper usersMapper, PasswordEncoder passwordEncoder) {
        this.usersMapper = usersMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<Users> findAll() {
        // null 表示不附加查询条件，因此会返回 users 表中的全部记录。
        return usersMapper.selectList(null);
    }

    @Override
    public Users findById(String id) {
        // MyBatis-Plus 根据实体主键字段执行查询；记录不存在时返回 null。
        return usersMapper.selectById(id);
    }

    @Override
    public boolean create(Users user) {
        // 在持久化前替换实体中的原始密码为 BCrypt 哈希；响应序列化也会隐藏 pw 字段。
        user.setPw(passwordEncoder.encode(user.getPw()));
        return usersMapper.insert(user) > 0;
    }

    @Override
    public boolean update(String id, Users user) {
        // 以 URL 路径中的 id 为准，避免请求体中的 id 改变本次更新目标。
        user.setId(id);
        if (user.getPw() != null) {
            // 空字符串代表本次不修改密码；非空新密码必须重新哈希后再更新。
            user.setPw(user.getPw().isBlank() ? null : passwordEncoder.encode(user.getPw()));
        }
        // updateById 根据实体主键更新；默认字段策略通常会忽略值为 null 的属性。
        return usersMapper.updateById(user) > 0;
    }

    @Override
    public boolean delete(String id) {
        // deleteById 返回实际删除的行数；ID 不存在时通常返回 0。
        return usersMapper.deleteById(id) > 0;
    }
}