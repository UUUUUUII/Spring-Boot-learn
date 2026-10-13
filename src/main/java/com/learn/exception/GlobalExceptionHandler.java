package com.learn.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 集中处理控制器执行过程中未被业务代码处理的异常。
 * {@code @RestControllerAdvice} 会把这里的方法应用到所有控制器，并将返回对象序列化为响应体。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Bean Validation 校验失败时，返回可定位到具体字段的错误列表。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "请求参数校验失败");
        problemDetail.setTitle("参数校验失败");
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> Map.of(
                "field", error.getField(),
                "message", Objects.toString(error.getDefaultMessage(), "参数不合法")))
                .toList();
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problemDetail);
    }

    /** JSON 缺失、格式错误或字段值无法反序列化为目标类型时，统一返回 400。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableRequestBody(
            HttpMessageNotReadableException exception) {
        return badRequest("请求体错误", "请求体为空、JSON 格式错误或字段类型不匹配");
    }

    /** 路径变量或查询参数无法转换成声明的 Java 类型时，统一返回 400。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return badRequest("参数类型错误", "参数 '" + exception.getName() + "' 类型不正确");
    }

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
            if (status.is5xxServerError()) {
                problemDetail.setDetail("服务器内部错误");
            } else if (problemDetail.getDetail() == null || problemDetail.getDetail().isBlank()) {
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

    private ResponseEntity<ProblemDetail> badRequest(String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle(title);
        return ResponseEntity.badRequest().body(problemDetail);
    }
}