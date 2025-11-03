package com.njumarket.njumarket.exception;

import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * 
 * 作用范围：
 * 1. 处理传播到Controller层的未捕获异常（兜底机制）
 * 2. 处理Spring框架层面的异常（参数绑定、数据验证等）
 * 3. 处理Service层忘记catch的异常
 * 
 * 注意：
 * - 如果Service层已经catch异常并返回Result.fail()，异常不会传播，GlobalExceptionHandler不会处理
 * - 当前架构：Service层是主要异常处理机制，GlobalExceptionHandler是兜底机制
 * - 这是"互补"关系，不是"覆写"关系
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 获取当前请求信息
     */
    private String getRequestInfo() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return String.format("path=%s, method=%s", request.getRequestURI(), request.getMethod());
            }
        } catch (Exception e) {
            // 忽略获取请求信息失败的情况
        }
        return "unknown";
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        String requestInfo = getRequestInfo();
        log.warn("业务异常: {}, request={}", e.getMessage(), requestInfo);
        return Result.fail(e.getMessage());
    }

    /**
     * 处理参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result handleIllegalArgumentException(IllegalArgumentException e) {
        String requestInfo = getRequestInfo();
        log.warn("参数异常: {}, request={}", e.getMessage(), requestInfo);
        return Result.fail("参数错误：" + e.getMessage());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result handleNullPointerException(NullPointerException e) {
        String requestInfo = getRequestInfo();
        log.error("空指针异常: request={}, error={}", requestInfo, e.getMessage(), e);
        return Result.fail("系统错误：数据缺失，请稍后重试");
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        String requestInfo = getRequestInfo();
        log.error("运行时异常: request={}, error={}", requestInfo, e.getMessage(), e);
        return Result.fail("系统错误，请稍后重试");
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        String requestInfo = getRequestInfo();
        log.error("系统异常: request={}, error={}, type={}", 
                requestInfo, e.getMessage(), e.getClass().getName(), e);
        return Result.fail("系统错误，请稍后重试");
    }
}
