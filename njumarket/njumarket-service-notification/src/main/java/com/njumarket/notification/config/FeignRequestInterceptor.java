package com.njumarket.notification.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Feign请求拦截器
 * 将当前请求的X-User-Id请求头传递给Feign请求
 * 用于服务间调用时传递用户身份信息
 */
@Slf4j
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 从当前请求上下文中获取请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // 提取X-User-Id请求头
            String userId = request.getHeader("X-User-Id");
            if (userId != null && !userId.trim().isEmpty()) {
                // 将X-User-Id添加到Feign请求头
                template.header("X-User-Id", userId);
                log.debug("Feign请求拦截器: 传递X-User-Id={} 到服务 {}", userId, template.feignTarget().name());
            } else {
                log.debug("Feign请求拦截器: 当前请求没有X-User-Id请求头");
            }
            
            // 可选：传递Authorization头（如果需要）
            String authorization = request.getHeader("Authorization");
            if (authorization != null && !authorization.trim().isEmpty()) {
                template.header("Authorization", authorization);
            }
        } else {
            log.debug("Feign请求拦截器: 无法获取请求上下文（可能是异步调用）");
        }
    }
}

