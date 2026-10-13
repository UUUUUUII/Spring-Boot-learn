package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.domain.Users;

/**
 * User 数据访问层。
 * 继承 BaseMapper 后即可使用 MyBatis-Plus 提供的常见 CRUD 方法，
 * 例如 selectById、selectList、insert、updateById 和 deleteById。
 */
public interface UsersMapper extends BaseMapper<Users> {
}
