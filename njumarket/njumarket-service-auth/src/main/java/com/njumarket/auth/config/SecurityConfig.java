package com.njumarket.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security配置
 * 重构说明：
 * 1. Gateway层已经完成JWT认证，Auth Service只需要设置用户上下文
 * 2. UserContextFilter会从X-User-Id头获取用户信息并设置SecurityContext
 * 3. 所有请求都允许通过，由UserContextFilter和Controller层的@CurrentUser注解进行权限控制
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（因为使用JWT）
            .csrf(csrf -> csrf.disable())
            // 使用无状态会话（JWT）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 内部API允许服务间调用，无需认证
                .requestMatchers("/api/internal/**").permitAll()
                // 公开API允许访问
                .requestMatchers("/api/public/**").permitAll()
                // 用户认证相关接口允许访问（登录、注册等）
                .requestMatchers("/api/user/auth/**").permitAll()
                // 其他请求允许通过（认证由Gateway和UserContextFilter处理）
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
}

