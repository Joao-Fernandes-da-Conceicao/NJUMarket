package com.njumarket.njumarket.resolver;

import com.njumarket.njumarket.annotation.CurrentUser;
import com.njumarket.njumarket.entity.User;
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
 * 符合Spring Security标准实践
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("无法获取当前用户：未认证");
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return principal;
        }
        
        log.warn("无法获取当前用户：Principal类型不匹配，类型为: {}", principal.getClass().getName());
        return null;
    }
}

