package com.njumarket.njumarket.aspect;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Service层日志切面
 * 统一记录Service方法的执行日志，包括方法开始、成功、异常等信息
 */
@Slf4j
@Aspect
@Component
public class ServiceLogAspect {

    /**
     * 定义切点：拦截所有Service实现类的public方法
     */
    @Pointcut("execution(public * com.njumarket.njumarket.service.impl.*.*(..))")
    public void serviceMethod() {
    }

    /**
     * 环绕通知：记录方法执行日志
     */
    @Around("serviceMethod()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 排除定时任务方法（@Scheduled注解的方法），避免产生过多日志
        if (method.isAnnotationPresent(Scheduled.class)) {
            // 定时任务直接执行，不记录日志
            return joinPoint.proceed();
        }
        
        String methodName = getMethodName(method);
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // 提取方法参数（简化显示，避免日志过长）
        Object[] args = joinPoint.getArgs();
        String params = formatParams(args);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 记录方法开始
            log.info("{}开始 - className={}, method={}, params={}", 
                    methodName, className, method.getName(), params);
            
            // 执行方法
            Object result = joinPoint.proceed();
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录方法成功
            String resultInfo = formatResult(result);
            log.info("{}成功 - className={}, method={}, result={}, executionTime={}ms", 
                    methodName, className, method.getName(), resultInfo, executionTime);
            
            return result;
            
        } catch (BusinessException e) {
            // 业务异常：记录为WARN级别
            long executionTime = System.currentTimeMillis() - startTime;
            log.warn("{}失败（业务异常） - className={}, method={}, error={}, executionTime={}ms", 
                    methodName, className, method.getName(), e.getMessage(), executionTime);
            // 业务异常直接抛出，不包装
            throw e;
            
        } catch (Exception e) {
            // 系统异常：记录为ERROR级别
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("{}失败（系统异常） - className={}, method={}, error={}, executionTime={}ms", 
                    methodName, className, method.getName(), e.getMessage(), executionTime, e);
            // 系统异常直接抛出，不包装（由Service方法或GlobalExceptionHandler处理）
            throw e;
        }
    }

    /**
     * 获取方法的中文名称（从方法名推断）
     */
    private String getMethodName(Method method) {
        String methodName = method.getName();
        
        // 常见方法名映射
        if (methodName.startsWith("create")) {
            return "创建" + extractEntityName(methodName.substring(6));
        } else if (methodName.startsWith("update")) {
            return "更新" + extractEntityName(methodName.substring(6));
        } else if (methodName.startsWith("delete")) {
            return "删除" + extractEntityName(methodName.substring(6));
        } else if (methodName.startsWith("get")) {
            return "获取" + extractEntityName(methodName.substring(3));
        } else if (methodName.startsWith("publish")) {
            return "发布" + extractEntityName(methodName.substring(7));
        } else if (methodName.startsWith("pay")) {
            return "支付" + extractEntityName(methodName.substring(3));
        } else if (methodName.startsWith("confirm")) {
            return "确认" + extractEntityName(methodName.substring(7));
        } else if (methodName.startsWith("cancel")) {
            return "取消" + extractEntityName(methodName.substring(6));
        } else if (methodName.startsWith("send")) {
            return "发送" + extractEntityName(methodName.substring(4));
        } else if (methodName.startsWith("mark")) {
            return "标记" + extractEntityName(methodName.substring(4));
        } else if (methodName.startsWith("search")) {
            return "搜索" + extractEntityName(methodName.substring(6));
        } else if (methodName.startsWith("request")) {
            return "申请" + extractEntityName(methodName.substring(7));
        } else if (methodName.startsWith("handle")) {
            return "处理" + extractEntityName(methodName.substring(6));
        } else if (methodName.startsWith("ship")) {
            return "发货" + extractEntityName(methodName.substring(4));
        } else if (methodName.equals("login")) {
            return "登录";
        } else if (methodName.equals("register")) {
            return "注册";
        } else if (methodName.equals("logout")) {
            return "登出";
        }
        
        // 默认返回方法名
        return methodName;
    }

    /**
     * 从方法名中提取实体名称
     */
    private String extractEntityName(String suffix) {
        if (suffix.isEmpty()) {
            return "";
        }
        
        // 常见实体名称映射
        if (suffix.startsWith("Order")) {
            return "订单";
        } else if (suffix.startsWith("Commodity")) {
            return "商品";
        } else if (suffix.startsWith("Message")) {
            return "消息";
        } else if (suffix.startsWith("Conversation")) {
            return "对话";
        } else if (suffix.startsWith("User")) {
            return "用户";
        } else if (suffix.startsWith("Profile")) {
            return "档案";
        } else if (suffix.startsWith("Refund")) {
            return "退款";
        } else if (suffix.startsWith("Return")) {
            return "退货";
        } else if (suffix.startsWith("Draft")) {
            return "草稿";
        }
        
        return suffix;
    }

    /**
     * 格式化方法参数（简化显示）
     */
    private String formatParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "无参数";
        }
        
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) {
                        return "null";
                    }
                    // 对于DTO对象，只显示关键字段
                    String className = arg.getClass().getSimpleName();
                    if (className.contains("DTO") || className.contains("Request")) {
                        return className + "(...)";
                    }
                    // 对于简单类型，直接显示
                    if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                        return String.valueOf(arg);
                    }
                    // 其他复杂对象，只显示类名
                    return className + "(...)";
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * 格式化返回值（简化显示）
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        if (result instanceof Result) {
            Result resultObj = (Result) result;
            if (Boolean.TRUE.equals(resultObj.getSuccess())) {
                Object data = resultObj.getData();
                if (data != null) {
                    String dataType = data.getClass().getSimpleName();
                    // 如果是分页对象，显示总数
                    if (dataType.contains("Page")) {
                        try {
                            Method getTotalElements = data.getClass().getMethod("getTotalElements");
                            long total = (Long) getTotalElements.invoke(data);
                            return "成功, total=" + total;
                        } catch (Exception e) {
                            // 忽略反射异常
                        }
                    }
                    return "成功, dataType=" + dataType;
                }
                return "成功";
            } else {
                return "失败: " + resultObj.getErrorMsg();
            }
        }
        
        return result.getClass().getSimpleName();
    }
}

