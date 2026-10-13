package com.learn.service;

import com.learn.domain.Users;

import java.util.List;

/**
 * User 业务层接口。
 * Controller 依赖该接口而不是具体实现，使 HTTP 处理逻辑与数据库访问细节分离。
 */
public interface UsersService {

    /** 查询全部用户；数据量较大时应扩展为分页查询。 */
    List<Users> findAll();

    /**
     * 按主键查询用户。
     * @return 找到时返回用户，找不到时返回 null
     */
    Users findById(String id);

    /**
     * 新增用户。
     * @return 数据库实际插入的行数大于 0 时返回 true
     */
    boolean create(Users user);

    /**
     * 按路径中的主键更新用户。
     * @return 数据库实际更新的行数大于 0 时返回 true
     */
    boolean update(String id, Users user);

    /**
     * 按主键删除用户。
     * @return 数据库实际删除的行数大于 0 时返回 true
     */
    boolean delete(String id);
}