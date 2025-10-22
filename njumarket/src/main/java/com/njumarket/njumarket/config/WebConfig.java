package com.njumarket.njumarket.config;

import com.njumarket.njumarket.interceptor.LoginInterceptor;
import com.njumarket.njumarket.interceptor.AdminInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户拦截器 - 只拦截用户相关路径
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/user/**")
                .excludePathPatterns(
                    // 用户认证相关接口不需要拦截
                    "/api/user/auth/login",
                    "/api/user/auth/register", 
                    "/api/user/auth/register-new",
                    "/api/user/auth/send-code",
                    "/api/user/auth/login-by-code",
                    "/api/user/auth/login-third-party",
                    "/api/user/auth/reset-password"
                );
        
        // 管理员拦截器 - 只拦截管理员相关路径
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                    // 管理员登录接口不需要拦截
                    "/api/admin/login"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 头像图片访问路径
        registry.addResourceHandler("/api/images/avatars/**")
                .addResourceLocations("file:uploads/avatars/");
    }

    /**
     * 配置multipart解析器
     */
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
