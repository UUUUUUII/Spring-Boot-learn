package com.learn.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 集中处理控制器执行过程中未被业务代码处理的异常。
 * {@code @RestControllerAdvice} 会把这里的方法应用到所有控制器，并将返回对象序列化为响应体。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获未单独处理的异常。
     * Spring MVC 的部分异常实现了 {@link ErrorResponse}，可从中取得框架建议的 HTTP 状态码。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception) {
        // 例如请求格式错误等框架异常应保留 4xx 状态，不能一概转换为 500。
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            ProblemDetail problemDetail = errorResponse.getBody();
            problemDetail.setTitle(status.is4xxClientError() ? "请求错误" : "服务器错误");
            if (problemDetail.getDetail() == null || problemDetail.getDetail().isBlank()) {
                problemDetail.setDetail(status.is4xxClientError()
                        ? "请求格式或参数不正确"
                        : "服务器内部错误");
            }
            // 保留 Spring 异常定义的 HTTP 状态码和响应头，并统一 ProblemDetail 标题。
            return ResponseEntity.status(status).headers(errorResponse.getHeaders()).body(problemDetail);
        }
        // 对未预期异常不向客户端暴露内部异常详情，避免泄露实现或环境信息。
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
        problemDetail.setTitle("服务器错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}