package com.njumarket.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security配置
 * 重构说明：
 * 1. Gateway层已经完成JWT认证，Admin Service只需要设置管理员上下文
 * 2. AdminContextFilter会从X-Admin-Id头获取管理员信息并设置SecurityContext
 * 3. 所有请求都允许通过，由AdminContextFilter和Controller层的@PreAuthorize注解进行权限控制
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用@PreAuthorize注解支持
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（因为使用JWT认证，不需要CSRF保护）
            .csrf(csrf -> csrf.disable())
            // 禁用Session（使用JWT无状态认证）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 登录接口公开访问
                .requestMatchers("/api/admin/login").permitAll()
                // 其他请求允许通过（认证由Gateway和AdminContextFilter处理）
                .anyRequest().permitAll()
            )
            // 禁用默认的登录页面重定向
            .formLogin(form -> form.disable())
            // 禁用HTTP Basic认证
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}

