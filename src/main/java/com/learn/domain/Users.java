package com.learn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 对应数据库中的 users 表。
 * 该类同时作为 MyBatis-Plus 的数据实体，以及 Controller 接收/返回 JSON 时使用的数据对象。
 */
@Data
@TableName("users")
public class Users {
    /** users 表的主键；创建用户时由请求提供，不在此处自动生成。 */
    @TableId("id")
    private String id;

    /** 用户名称，对应 users 表中的 name 列。 */
    private String name;

    /**
     * 用户密码字段。WRITE_ONLY 表示允许从请求 JSON 反序列化密码，但序列化响应 JSON 时不输出该字段。
     * 这只是避免接口回传密码，不等于对密码进行加密或哈希存储。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pw;

    /** 用户角色，对应 users 表中的 role 列。 */
    private String role;

    /** 用户状态，例如可用/禁用；具体取值约定由业务规则决定。 */
    private Integer active;
}
