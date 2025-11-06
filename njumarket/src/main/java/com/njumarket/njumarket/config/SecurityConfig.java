package com.njumarket.njumarket.config;

import com.njumarket.njumarket.filter.JwtAuthenticationFilter;
import com.njumarket.njumarket.filter.AdminAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置类
 * 使用JWT Filter进行用户和管理员认证，启用方法级安全控制
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用方法级安全控制，支持@PreAuthorize等注解
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AdminAuthenticationFilter adminAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 用户认证相关接口公开访问
                .requestMatchers(
                    "/api/user/auth/login",
                    "/api/user/auth/register",
                    "/api/user/auth/register-new",
                    "/api/user/auth/send-code",
                    "/api/user/auth/login-by-code",
                    "/api/user/auth/login-third-party",
                    "/api/user/auth/reset-password"
                ).permitAll()
                // 管理员登录接口公开访问
                .requestMatchers("/api/admin/login").permitAll()
                // 用户相关接口和联系功能接口需要JWT认证
                .requestMatchers("/api/user/**", "/api/contact/**").authenticated()
                // 管理员相关接口需要管理员认证
                .requestMatchers("/api/admin/**").authenticated()
                // 其他接口允许访问
                .anyRequest().permitAll()
            )
            // 先添加JWT Filter到UsernamePasswordAuthenticationFilter之前
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 然后将Admin Filter添加到JWT Filter之前（管理员路径优先处理）
            .addFilterBefore(adminAuthenticationFilter, JwtAuthenticationFilter.class);
        
        return http.build();
    }
}
