package com.learn.controller;

import com.learn.domain.Users;
import com.learn.service.UsersService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User REST API 控制器。
 * 负责将 HTTP 路由、请求参数和响应状态码转换为对 UsersService 的调用；业务和数据库逻辑留在下层。
 */
@RestController
// 所有接口共享此前缀，例如 GET /api/users/{id}。
@RequestMapping("/api/users")
public class UsersController {

    /** 通过构造器注入业务层接口，避免控制器直接依赖 Mapper。 */
    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    /** GET /api/users：查询全部用户，结果由 Spring 自动序列化为 JSON 数组。 */
    @GetMapping
    public List<Users> findAll() {
        return usersService.findAll();
    }

    /** GET /api/users/{id}：按主键查询；存在时返回 200，不存在时返回 404。 */
    @GetMapping("/{id}")
    public ResponseEntity<Users> findById(@PathVariable String id) {
        Users user = usersService.findById(id);
        return user == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(user);
    }

    /**
     * POST /api/users：创建用户。
     * 请求体使用 JSON，当前实体的字符串主键需要由调用方提供；成功时返回 201 和新建用户。
     */
    @PostMapping
    public ResponseEntity<Users> create(@RequestBody Users user) {
        // 空 ID 无法定位 users 表的主键，因此在进入业务层前返回 400 Bad Request。
        if (user.getId() == null || user.getId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // 正常插入返回 201；Mapper 未插入记录时返回 500。数据库异常通常会由 Spring 转为错误响应。
        return usersService.create(user)
                ? ResponseEntity.status(HttpStatus.CREATED).body(user)
                : ResponseEntity.internalServerError().build();
    }

    /**
     * PUT /api/users/{id}：根据路径 ID 更新用户。
     * 成功时返回 204（无响应体）；目标记录不存在时返回 404。
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable String id, @RequestBody Users user) {
        return usersService.update(id, user)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /** DELETE /api/users/{id}：按主键删除；成功返回 204，记录不存在返回 404。 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return usersService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}