package com.njumarket.njumarket.exception;

import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回统一的Result格式
 * 
 * 注意：此处理器仅用于Spring MVC应用（Servlet），不适用于Spring WebFlux应用（Gateway）
 * 
 * 异常处理优先级（从具体到通用）：
 * 1. 业务异常（BusinessException）
 * 2. 参数验证异常（MethodArgumentNotValidException, ConstraintViolationException, BindException）
 * 3. 参数类型异常（MethodArgumentTypeMismatchException, MissingServletRequestParameterException）
 * 4. HTTP请求异常（HttpRequestMethodNotSupportedException, HttpMediaTypeNotSupportedException）
 * 5. 空指针异常（NullPointerException）
 * 6. 参数非法异常（IllegalArgumentException, IllegalStateException）
 * 7. 通用系统异常（Exception）
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * 业务异常通常返回HTTP 200，由前端根据success字段判断
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    /**
     * 处理参数验证异常（@RequestBody + @Valid）
     * 当Controller方法参数使用@Valid注解时，验证失败会抛出此异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数验证失败: {}", message);
        return Result.fail("参数验证失败: " + message);
    }

    /**
     * 处理参数验证异常（@RequestParam, @PathVariable + @Valid）
     * 当方法参数使用@Valid注解时，验证失败会抛出此异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数验证失败: {}", message);
        return Result.fail("参数验证失败: " + message);
    }

    /**
     * 处理绑定异常
     * 当请求参数无法绑定到对象时抛出此异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.fail("参数绑定失败: " + message);
    }

    /**
     * 处理参数类型不匹配异常
     * 当请求参数类型无法转换为方法参数类型时抛出此异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数 '%s' 类型不匹配，期望类型: %s，实际值: %s",
                e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知",
                e.getValue());
        log.warn("参数类型不匹配: {}", message);
        return Result.fail("参数类型错误: " + message);
    }

    /**
     * 处理缺少必需参数异常
     * 当请求缺少必需的请求参数时抛出此异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = String.format("缺少必需参数: %s (类型: %s)", e.getParameterName(), e.getParameterType());
        log.warn("缺少必需参数: {}", message);
        return Result.fail("请求参数不完整: " + message);
    }

    /**
     * 处理HTTP请求方法不支持异常
     * 当请求使用了不支持的HTTP方法时抛出此异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = String.format("请求方法 '%s' 不支持，支持的方法: %s",
                e.getMethod(),
                String.join(", ", e.getSupportedMethods()));
        log.warn("HTTP方法不支持: {}", message);
        return Result.fail("请求方法不支持: " + message);
    }

    /**
     * 处理HTTP媒体类型不支持异常
     * 当请求的Content-Type不支持时抛出此异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        String message = String.format("媒体类型 '%s' 不支持，支持的类型: %s",
                e.getContentType(),
                e.getSupportedMediaTypes());
        log.warn("媒体类型不支持: {}", message);
        return Result.fail("请求格式不支持: " + message);
    }

    /**
     * 处理HTTP消息不可读异常
     * 当请求体格式错误（如JSON格式错误）时抛出此异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "请求体格式错误，请检查JSON格式是否正确";
        log.warn("请求体格式错误: {}", e.getMessage());
        return Result.fail(message);
    }

    /**
     * 处理404异常（处理器未找到）
     * 当请求的URL不存在时抛出此异常
     * 注意：NoHandlerFoundException 是 Spring MVC 特有的异常，在 WebFlux 中不存在
     */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleNoHandlerFoundException(org.springframework.web.servlet.NoHandlerFoundException e) {
        String message = String.format("请求的资源不存在: %s %s", e.getHttpMethod(), e.getRequestURL());
        log.warn("资源未找到: {}", message);
        return Result.fail("请求的资源不存在");
    }

    /**
     * 处理空指针异常
     * 空指针异常通常是代码问题，需要记录详细堆栈信息以便排查
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleNullPointerException(NullPointerException e) {
        log.error("空指针异常，请检查代码逻辑", e);
        return Result.fail("系统错误，请稍后重试");
    }

    /**
     * 处理非法参数异常
     * 当方法接收到非法参数时抛出此异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数异常: {}", e.getMessage());
        return Result.fail("参数错误: " + e.getMessage());
    }

    /**
     * 处理非法状态异常
     * 当对象处于不适当的状态时抛出此异常
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleIllegalStateException(IllegalStateException e) {
        log.warn("非法状态异常: {}", e.getMessage());
        return Result.fail("操作失败: " + e.getMessage());
    }

    /**
     * 处理数组越界异常
     * 当访问数组时索引超出范围时抛出此异常
     */
    @ExceptionHandler(ArrayIndexOutOfBoundsException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleArrayIndexOutOfBoundsException(ArrayIndexOutOfBoundsException e) {
        log.error("数组越界异常，请检查代码逻辑", e);
        return Result.fail("系统错误，请稍后重试");
    }

    /**
     * 处理类型转换异常
     * 当类型转换失败时抛出此异常
     */
    @ExceptionHandler(ClassCastException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleClassCastException(ClassCastException e) {
        log.error("类型转换异常，请检查代码逻辑", e);
        return Result.fail("系统错误，请稍后重试");
    }

    /**
     * 处理数字格式异常
     * 当字符串转换为数字失败时抛出此异常
     */
    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleNumberFormatException(NumberFormatException e) {
        log.warn("数字格式异常: {}", e.getMessage());
        return Result.fail("数字格式错误: " + e.getMessage());
    }

    /**
     * 处理系统异常
     * 捕获所有未处理的异常，避免暴露系统内部错误
     * 此处理器应该放在最后，作为兜底处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.fail("系统错误，请稍后重试");
    }
}

