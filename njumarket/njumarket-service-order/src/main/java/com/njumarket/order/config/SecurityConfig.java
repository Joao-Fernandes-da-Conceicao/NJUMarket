package com.njumarket.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security配置
 * 参考单体版（1.4.1）的SecurityConfig实现
 * 
 * 设计说明：
 * 1. 使用无状态会话（JWT）
 * 2. 禁用CSRF（因为使用JWT）
 * 3. 允许所有请求（认证由UserContextFilter处理，Gateway已验证JWT）
 * 4. 不配置Filter链（UserContextFilter通过@Component自动注册）
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
            // 允许所有请求（认证由UserContextFilter处理）
            // Gateway已经验证了JWT，这里只需要设置SecurityContext
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        
        return http.build();
    }
}

