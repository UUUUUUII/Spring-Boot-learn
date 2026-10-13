package com.learn.controller;

import com.learn.common.ApiResponse;
import com.learn.domain.Users;
import com.learn.service.UsersService;
import jakarta.validation.Valid;
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
import org.springframework.web.server.ResponseStatusException;

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

    /** GET /api/users：查询全部用户。 */
    @GetMapping
    public ApiResponse<List<Users>> findAll() {
        return ApiResponse.success(usersService.findAll());
    }

    /** GET /api/users/{id}：按主键查询；无论是否命中都返回 200，未命中时 data 为 null。 */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Users>> findById(@PathVariable String id) {
        Users user = usersService.findById(id);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
    /**
     * POST /api/users/create：创建用户。
     * 请求体为经过字段校验的 JSON 用户实体；业务层会在写入数据库前对密码做 BCrypt 哈希。
     *
     * @param user 请求体中的新用户信息，字符串主键按当前实体约定由调用方提供
     * @return HTTP 200 和新建用户的统一响应；响应序列化不会包含密码字段
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Users>> create(@Valid @RequestBody Users user) {
        // Mapper 未插入任何记录时显式转为 500；数据库抛出的异常交由全局异常处理器转换。
        if (!usersService.create(user)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建用户失败");
        }
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(user));
    }

    /**
     * PUT /api/users/{id}：根据路径中的主键更新用户信息。
     * 路径 ID 优先于请求体中的 ID；密码字段为空或未提供时不修改现有密码。
     *
     * @param id URL 路径中的目标用户主键
     * @param user 请求体中的待更新字段
     * @return 更新成功时返回 HTTP 200 和空数据统一响应；没有更新记录时返回 404
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable String id, @RequestBody Users user) {
        if (!usersService.update(id, user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return ResponseEntity.ok(ApiResponse.emptySuccess());
    }

    /**
     * DELETE /api/users/{id}：按 URL 路径中的主键删除用户。
     *
     * @param id URL 路径中的目标用户主键
     * @return 删除成功时返回 HTTP 200 和空数据统一响应；没有删除记录时返回 404
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        if (!usersService.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return ResponseEntity.ok(ApiResponse.emptySuccess());
    }
}