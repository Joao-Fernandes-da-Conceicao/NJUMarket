package com.njumarket.njumarket.resolver;

import com.njumarket.njumarket.annotation.CurrentUser;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 当前用户参数解析器 - 将@CurrentUser注解的参数解析为User对象
 * 参考单体版（1.4.1）的实现
 * 
 * 设计说明：
 * 1. 优先从Spring Security SecurityContext获取（标准方式）
 * 2. 向后兼容UserHolder（用于没有Spring Security的场景）
 * 3. 使用@Component自动注册，无需显式配置
 */
@Slf4j
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
               parameter.getParameterType().equals(User.class);
    }

    @Override
    @Nullable
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  @Nullable WebDataBinderFactory binderFactory) throws Exception {
        // 优先从Spring Security SecurityContext获取（标准方式）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        log.debug("CurrentUserArgumentResolver.resolveArgument: authentication={}, isAuthenticated={}", 
            authentication != null, authentication != null && authentication.isAuthenticated());
        
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            log.debug("CurrentUserArgumentResolver: principal类型={}, 是否为User实例={}", 
                principal != null ? principal.getClass().getName() : "null", principal instanceof User);
            
            if (principal instanceof User) {
                User user = (User) principal;
                log.info("CurrentUserArgumentResolver: 从SecurityContext获取用户成功, userId={}", user.getUserId());
                return user;
            } else {
                log.warn("CurrentUserArgumentResolver: Principal类型不匹配, 期望User, 实际={}", 
                    principal != null ? principal.getClass().getName() : "null");
            }
        } else {
            log.debug("CurrentUserArgumentResolver: SecurityContext中没有认证信息");
        }
        
        // 向后兼容：从UserHolder获取（用于没有Spring Security的场景）
        User user = UserHolder.getUser();
        if (user != null) {
            log.info("CurrentUserArgumentResolver: 从UserHolder获取用户成功, userId={}", user.getUserId());
        } else {
            log.warn("CurrentUserArgumentResolver: 无法获取当前用户：SecurityContext和UserHolder都为空");
        }
        return user;
    }
}

