package com.learn.service.impl;

import com.learn.domain.Users;
import com.learn.mapper.UsersMapper;
import com.learn.service.UsersService;
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

    /** Spring 会自动注入容器中的 UsersMapper 实例。 */
    public UsersServiceImpl(UsersMapper usersMapper) {
        this.usersMapper = usersMapper;
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
        // insert 返回影响的行数；大于 0 代表至少有一行被写入。
        return usersMapper.insert(user) > 0;
    }

    @Override
    public boolean update(String id, Users user) {
        // 以 URL 路径中的 id 为准，避免请求体中的 id 改变本次更新目标。
        user.setId(id);
        // updateById 根据实体主键更新；默认字段策略通常会忽略值为 null 的属性。
        return usersMapper.updateById(user) > 0;
    }

    @Override
    public boolean delete(String id) {
        // deleteById 返回实际删除的行数；ID 不存在时通常返回 0。
        return usersMapper.deleteById(id) > 0;
    }
}