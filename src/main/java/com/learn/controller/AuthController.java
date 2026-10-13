package com.learn.controller; // 将登录控制器归入应用的 HTTP 控制器包。

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 用类型安全的实体属性引用构造用户名查询条件。
import com.learn.auth.AuthenticatedUser; // 表示验证通过后可放入 Session 并写入 JWT 的最小用户身份。
import com.learn.auth.JwtTokenService; // 负责签发登录成功后返回的 JWT。
import com.learn.auth.LoginRequest; // 定义并校验客户端提交的用户名和原始密码。
import com.learn.auth.LoginResponse; // 定义返回给客户端的 JWT、有效期和 CSRF token。
import com.learn.common.ApiResponse; // 将登录成功数据包装为项目统一响应格式。
import com.learn.domain.Users; // 映射数据库中的用户记录。
import com.learn.interceptor.AuthenticationInterceptor; // 复用拦截器定义的 Session 属性键。
import com.learn.mapper.UsersMapper; // 查询用户名对应的用户记录。
import jakarta.servlet.http.HttpServletRequest; // 获取登录前已有的 Servlet Session。
import jakarta.servlet.http.HttpSession; // 创建并写入登录成功后的 Session。
import jakarta.validation.Valid; // 在调用登录方法前触发请求字段校验。
import org.springframework.http.HttpStatus; // 表示凭证错误时使用的 HTTP 401 状态。
import org.springframework.http.ResponseEntity; // 显式设置登录响应的 HTTP 状态和响应体。
import org.springframework.security.crypto.password.PasswordEncoder; // 安全地将原始密码与 BCrypt 哈希进行比对。
import org.springframework.web.bind.annotation.PostMapping; // 将登录处理方法映射为 POST 请求。
import org.springframework.web.bind.annotation.RequestBody; // 将 JSON 请求体反序列化为 LoginRequest。
import org.springframework.web.bind.annotation.RequestMapping; // 为本控制器下的路由声明统一前缀。
import org.springframework.web.bind.annotation.RestController; // 将本类注册为返回响应数据的 Spring MVC 控制器。
import org.springframework.web.server.ResponseStatusException; // 将认证失败转换为指定 HTTP 状态的异常。

import java.security.SecureRandom; // 生成不可预测的 Session CSRF token。
import java.util.Base64; // 将随机 token 编码为适合放入 HTTP 头的文本。

/**
 * 提供用户名密码登录入口。
 * 登录成功会同时创建新的服务端 Session 并签发 JWT，客户端之后可任选一种凭证访问受保护 API。
 */
@RestController // 注册为 REST 控制器，使方法返回值写入 HTTP 响应体。
@RequestMapping("/api/auth") // 为本控制器的接口统一添加 /api/auth 路径前缀。
public class AuthController { // 定义处理登录请求及建立登录凭证的控制器。

    /** 数据访问依赖，用于按用户名查找登录账号。 */
    private final UsersMapper usersMapper; // 查询登录名对应的用户数据库记录。
    /** BCrypt 编码器，仅用于安全比对原始密码与存储哈希。 */
    private final PasswordEncoder passwordEncoder; // 比对用户提交的密码与数据库中保存的哈希。
    /** JWT 签发服务。 */
    private final JwtTokenService jwtTokenService; // 根据验证通过的身份签发 JWT。
    /** 使用密码学安全随机数生成每个 Session 独有的 CSRF token。 */
    private final SecureRandom secureRandom = new SecureRandom(); // 由 JDK 提供密码学安全随机数生成器，供 CSRF token 使用。

    /**
     * 通过构造器注入登录所需组件，便于测试时替换依赖。
     *
     * @param usersMapper 用户数据访问对象
     * @param passwordEncoder BCrypt 密码编码器
     * @param jwtTokenService JWT 签发服务
     */
    public AuthController(UsersMapper usersMapper, PasswordEncoder passwordEncoder, // 接收 Spring 注入的用户查询器和密码编码器。
                          JwtTokenService jwtTokenService) { // 同时接收 JWT 服务，集中声明本控制器的必需依赖。
        this.usersMapper = usersMapper; // 保存用户查询器，供登录时按用户名查找账号。
        this.passwordEncoder = passwordEncoder; // 保存密码编码器，供登录时校验密码。
        this.jwtTokenService = jwtTokenService; // 保存 JWT 服务，供凭证校验成功后签发令牌。
    } // 构造器结束；Spring 创建控制器时会调用此构造器。

    /**
     * 按用户名查找账号并校验原始密码；成功后轮换 Session、生成 CSRF token 并签发 JWT。
     * 用户名不存在或密码错误时统一返回 401，避免通过响应差异枚举已注册账号。
     *
     * @param loginRequest 请求体中的用户名和原始密码；字段校验由 {@code @Valid} 在方法调用前执行
     * @param request 当前 Servlet 请求，用于使已有 Session 失效并为本次登录创建新 Session
     * @return HTTP 200 和含 JWT、到期时间及 Session CSRF token 的统一响应；Servlet 容器同时设置 Session Cookie
     */
    @PostMapping("/login") // 将此方法绑定到 POST /api/auth/login。
    public ResponseEntity<ApiResponse<LoginResponse>> login( // 返回 HTTP 响应，其中业务数据按统一格式包装。
            @Valid @RequestBody LoginRequest loginRequest, // 读取 JSON 登录凭证，并在方法执行前验证非空字段。
            HttpServletRequest request) { // 提供当前请求，以便读取并替换登录前已存在的 Session。
        Users user = usersMapper.selectOne(new LambdaQueryWrapper<Users>() // 查询至多一条与请求用户名匹配的用户记录。
                .eq(Users::getName, loginRequest.name())); // 使用实体属性引用按用户名精确匹配数据库记录。
        if (user == null || !passwordEncoder.matches(loginRequest.pw(), user.getPw())) { // 账号不存在或密码哈希不匹配时均视为认证失败。
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"); // 两种失败共用同一状态和消息，避免泄露账号是否存在。
        } // 凭证不匹配时在此结束；只有验证成功才继续创建登录凭证。

        // 仅在用户名和密码都验证通过后构造身份对象，且不把密码复制到身份信息中。
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getName(), user.getRole()); // 仅保留主键、用户名和角色供后续认证使用。
        HttpSession oldSession = request.getSession(false); // 只查找已有 Session，不因登录尝试本身提前创建 Session。
        if (oldSession != null) { // 仅当客户端已有 Session 时才执行会话轮换。
            oldSession.invalidate(); // 使旧会话失效，避免登录后继续使用登录前的会话标识。
        } // 完成旧 Session 清理；不存在旧 Session 时直接进入新 Session 创建步骤。
        HttpSession session = request.getSession(true); // 创建本次登录使用的新 Session，并由容器维护其 Cookie。
        session.setAttribute(AuthenticationInterceptor.SESSION_USER_ATTRIBUTE, authenticatedUser); // 保存已认证身份，供拦截器识别 Session 登录。

        byte[] csrfBytes = new byte[32]; // 分配 32 字节随机数据，作为本次 Session 的 CSRF token 原始值。
        secureRandom.nextBytes(csrfBytes); // 使用密码学安全随机数填充数组，避免 token 可预测。
        String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(csrfBytes); // 使用 URL 安全 Base64 编码，且移除填充符以便放入请求头。
        session.setAttribute(AuthenticationInterceptor.SESSION_CSRF_ATTRIBUTE, csrfToken); // 将 token 绑定到该 Session，供后续写请求进行校验。

        JwtTokenService.IssuedJwt issuedJwt = jwtTokenService.issue(authenticatedUser); // 为同一已认证身份签发 JWT，并同时取得其过期时间。
        LoginResponse response = new LoginResponse(issuedJwt.value(), "Bearer", issuedJwt.expiresAt(), csrfToken); // 组合 JWT 和 Session CSRF token 返回值。
        return ResponseEntity.ok(ApiResponse.success(response)); // 以 HTTP 200 和统一成功响应返回登录凭证。
    } // 登录处理方法结束。
} // AuthController 类定义结束。