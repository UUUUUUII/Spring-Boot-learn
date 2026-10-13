# Spring-Boot-learn

一个用于学习 Spring Boot 后端开发的示例项目，提供基于 MySQL 的用户 REST API，覆盖控制器、业务层、数据访问层、参数校验和异常处理。

## 技术栈

- Java 25
- Spring Boot 4.1.1、Spring MVC
- MyBatis-Plus 3.5.17
- MySQL、HikariCP
- Jakarta Bean Validation、Lombok
- Maven Wrapper

## 项目结构

```text
src/main/java/com/learn/
├── SpringBootLearnApplication.java  # 应用入口和 Mapper 扫描
├── common/ApiResponse.java          # 统一成功响应
├── controller/UsersController.java  # 用户 HTTP 接口
├── domain/Users.java                # users 表实体及请求校验
├── exception/GlobalExceptionHandler.java # 全局异常处理
├── mapper/UsersMapper.java          # MyBatis-Plus 数据访问
└── service/
		├── UsersService.java            # 业务层接口
		└── impl/UsersServiceImpl.java   # 业务层实现
src/main/resources/application.yaml  # 应用和数据库配置
src/test/java/com/learn/             # 测试
```

请求处理流程为：`UsersController` → `UsersService` → `UsersMapper` → MySQL。控制器负责 HTTP 路由和状态码，业务层调用 MyBatis-Plus，Mapper 继承 `BaseMapper<Users>` 获得常用 CRUD 操作。

## 用户接口

所有接口前缀为 `/api/users`，默认服务端口为 `8080`。

| 方法 | 路径 | 说明 | 成功状态 |
| --- | --- | --- | --- |
| GET | `/api/users` | 查询全部用户 | 200 |
| GET | `/api/users/{id}` | 按 ID 查询；不存在时 `data` 为 `null` | 200 |
| POST | `/api/users` | 创建用户 | 200 |
| PUT | `/api/users/{id}` | 更新用户；ID 以路径参数为准 | 200 |
| DELETE | `/api/users/{id}` | 删除用户 | 200 |

用户实体字段：`id`、`name`、`pw`、`role`、`active`。`name` 和 `pw` 不能为空；`role` 默认值为 `user`，`active` 默认值为 `1`。响应 JSON 不会包含 `pw`，但这不代表数据库中的密码已经加密。

成功响应使用统一格式：

```json
{
	"code": 0,
	"success": true,
	"message": "success",
	"data": {}
}
```

请求校验失败返回 HTTP 400，重复键冲突返回 409，更新或删除不存在的用户返回 404。异常响应由全局异常处理器返回 Spring `ProblemDetail` 格式，不使用上述成功响应包装。

> **ID 配置注意：** `Users.id` 当前标注为 MyBatis-Plus 的 `IdType.AUTO`，但创建接口注释写明 ID 由调用方提供。请按实际数据库主键配置核对并统一这两处约定，再确定创建请求是否需要传入 `id`。

## 本地运行

### 环境准备

- 安装 JDK 25，并确保 `java -version` 指向该版本。
- 准备 MySQL，并创建数据库 `test` 和兼容实体字段的 `users` 表。
- 修改 `src/main/resources/application.yaml` 中的数据库 URL、用户名和密码；当前示例连接本机 `test` 数据库，运行前请使用自己的本地配置，勿将真实凭据提交到版本库。

### 启动和测试

Windows 命令提示符或 PowerShell：

```powershell
./mvnw.cmd spring-boot:run
./mvnw.cmd test
```

打包应用：

```powershell
./mvnw.cmd package
```

启动后可通过 `http://localhost:8080/api/users` 访问用户列表接口。
