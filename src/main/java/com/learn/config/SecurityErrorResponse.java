package com.learn.config;

import com.learn.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 将 Spring Security 认证和授权错误转换为项目统一的 JSON 响应。 */
public final class SecurityErrorResponse {

    private SecurityErrorResponse() {
    }

    /** 设置状态码、JSON 编码和响应体，避免未认证请求落入默认 HTML 错误页。 */
    public static void write(ObjectMapper objectMapper, HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(message));
    }
}