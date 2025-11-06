package com.njumarket.njumarket.config;

import com.njumarket.njumarket.resolver.CurrentUserArgumentResolver;
import com.njumarket.njumarket.resolver.CurrentAdminArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC配置 - 注册自定义参数解析器
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final CurrentAdminArgumentResolver currentAdminArgumentResolver;

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
        resolvers.add(currentAdminArgumentResolver);
    }
}

