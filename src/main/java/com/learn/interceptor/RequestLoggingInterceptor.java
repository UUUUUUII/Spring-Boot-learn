package com.learn.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 记录每个 HTTP 请求的处理耗时和最终状态码。
 *
 * <p>拦截器通常由 Spring 作为单例复用，因此请求开始时间不能保存在实例字段中，
 * 否则并发请求可能相互覆盖。这里将时间戳放在当前请求的属性中，供请求完成回调读取。</p>
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    /** 当前请求中用于传递开始时间的属性名。 */
    private static final String START_TIME_ATTRIBUTE = RequestLoggingInterceptor.class.getName() + ".START_TIME";

    /**
     * 在 Spring MVC 调用控制器之前记录当前请求的单调时钟起点。
     * 起点写入 request attribute，供同一请求的 {@link #afterCompletion} 计算耗时；本方法不拦截请求。
     *
     * @param request 当前 HTTP 请求，用于保存本次请求的开始时间
     * @param response 当前 HTTP 响应；此处不修改响应内容
     * @param handler Spring MVC 解析出的处理器
     * @return 始终返回 {@code true}，继续执行后续拦截器和控制器
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
        return true;
    }

    /**
     * 请求完成后记录方法、URI、最终响应状态和处理耗时；控制器正常返回或抛出异常后都会进入此回调。
     * 如果请求属性中没有本拦截器记录的开始时间，则跳过日志，避免对缺失或被覆盖的属性做错误转换。
     *
     * @param request 已完成处理的 HTTP 请求，包含 {@link #preHandle} 保存的计时起点
     * @param response 已完成处理的 HTTP 响应，提供最终状态码
     * @param handler Spring MVC 解析出的处理器
     * @param exception 请求处理期间抛出的异常；日志记录使用最终响应状态，不输出异常内容
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception exception) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        // 正常情况下 preHandle 已设置该值；类型检查也避免属性被其他代码意外覆盖时发生强制转换异常。
        if (!(startTime instanceof Long startedAt)) {
            return;
        }

        // 使用 nanoTime 计算时间间隔，避免系统时钟校准对耗时结果造成影响。
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("{} {} completed with status {} in {} ms",
                request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
    }
}