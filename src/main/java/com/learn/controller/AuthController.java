package com.learn.controller; // 将登录控制器归入应用的 HTTP 控制器包。

import com.learn.auth.AuthenticatedUserDetails;
import com.learn.auth.JwtTokenService; // 负责签发登录成功后返回的 JWT。
import com.learn.auth.LoginRequest; // 定义并校验客户端提交的用户名和原始密码。
import com.learn.auth.LoginResponse; // 定义返回给客户端的 JWT 和有效期。
import com.learn.common.ApiResponse; // 将登录成功数据包装为项目统一响应格式。
import jakarta.validation.Valid; // 在调用登录方法前触发请求字段校验。
import org.springframework.http.ResponseEntity; // 显式设置登录响应的 HTTP 状态和响应体。
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping; // 将登录处理方法映射为 POST 请求。
import org.springframework.web.bind.annotation.RequestBody; // 将 JSON 请求体反序列化为 LoginRequest。
import org.springframework.web.bind.annotation.RequestMapping; // 为本控制器下的路由声明统一前缀。
import org.springframework.web.bind.annotation.RestController; // 将本类注册为返回响应数据的 Spring MVC 控制器。
import org.springframework.http.HttpStatus;

/**
 * 提供用户名密码登录入口。
 * 使用 Spring Security 校验用户名密码，登录成功后签发 JWT。
 */
@RestController // 注册为 REST 控制器，使方法返回值写入 HTTP 响应体。
@RequestMapping("/api/auth") // 为本控制器的接口统一添加 /api/auth 路径前缀。
public class AuthController { // 定义处理登录请求及建立登录凭证的控制器。

    /** 接收未认证的用户名密码凭证，并委托 Provider 完成查询和密码校验。 */
    private final AuthenticationManager authenticationManager;
    /** JWT 签发服务。 */
    private final JwtTokenService jwtTokenService; // 根据验证通过的身份签发 JWT。
    /**
     * 通过构造器注入登录所需组件，便于测试时替换依赖。
     *
    * @param authenticationManager Spring Security 认证管理器
     * @param jwtTokenService JWT 签发服务
     */
    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService; // 保存 JWT 服务，供凭证校验成功后签发令牌。
    } // 构造器结束；Spring 创建控制器时会调用此构造器。

    /**
        * 使用 Spring Security 验证用户名和密码，成功后签发 JWT。
     *
     * @param loginRequest 请求体中的用户名和原始密码；字段校验由 {@code @Valid} 在方法调用前执行
        * @return HTTP 200 和含 JWT 及到期时间的统一响应
     */
    @PostMapping("/login") // 将此方法绑定到 POST /api/auth/login。
    public ResponseEntity<ApiResponse<LoginResponse>> login( // 返回 HTTP 响应，其中业务数据按统一格式包装。
            @Valid @RequestBody LoginRequest loginRequest) {
        try {
            // unauthenticated token 表示尚未验证的登录凭证；认证管理器会调用用户服务和密码编码器。
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.name(), loginRequest.pw()));
            if (!(authentication.getPrincipal() instanceof AuthenticatedUserDetails userDetails)) {
                throw new IllegalStateException("认证管理器返回了不支持的用户主体");
            }
            // 只把不含密码的业务身份写入 JWT，再将令牌和有效期返回给客户端。
            JwtTokenService.IssuedJwt issuedJwt = jwtTokenService.issue(userDetails.getUser());
            LoginResponse response = new LoginResponse(issuedJwt.value(), "Bearer", issuedJwt.expiresAt());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AuthenticationException _) {
            // 不区分用户名不存在和密码错误，避免响应泄露账号是否存在。
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("用户名或密码错误"));
        }
    } // 登录处理方法结束。
} // AuthController 类定义结束。