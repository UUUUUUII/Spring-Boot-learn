package com.learn.common;

/**
 * API 响应体的统一格式；业务 code 与 HTTP 状态码相互独立。
 */
public record ApiResponse<T>(int code, boolean success, String message, T data) {

    /** 业务处理成功；HTTP 状态码仍由 ResponseEntity 决定。 */
    private static final int SUCCESS_CODE = 0;
    private static final int DEFAULT_FAILURE_CODE = 1;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, true, "success", data);
    }

    public static ApiResponse<Void> emptySuccess() {
        return success(null);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return failure(DEFAULT_FAILURE_CODE, message);
    }

    /** 创建带明确业务错误码的失败响应；调用方应传入非零 code。 */
    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, false, message, null);
    }
}