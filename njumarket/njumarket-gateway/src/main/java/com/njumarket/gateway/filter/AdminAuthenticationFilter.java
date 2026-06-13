package com.njumarket.gateway.filter;

import com.njumarket.njumarket.utils.JwtUtils;
import com.njumarket.njumarket.web.AuthCookieNames;
import org.springframework.http.HttpCookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 管理员认证Gateway Filter（响应式）
 * 在Gateway层统一处理管理员JWT认证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestURI = request.getURI().getPath();

        // 只处理管理员相关路径（前端直接调用 /api/admin/**）
        if (!requestURI.startsWith("/api/admin/")) {
            return chain.filter(exchange);
        }

        // 排除登录接口（管理员只有一个公开接口：登录）
        if (requestURI.equals("/api/admin/login")) {
            return chain.filter(exchange);
        }

        // 其他所有管理员接口都需要JWT认证

        // 1. 从请求头获取Token
        String token = getTokenFromRequest(request);
        
        if (!StringUtils.hasText(token)) {
            log.warn("管理员请求缺少Authorization头: {}", requestURI);
            return unauthorizedResponse(exchange, "管理员未登录，请先登录");
        }

        // 2. 验证Token有效性
        if (!jwtUtils.validateToken(token)) {
            log.warn("管理员Token验证失败: {}", requestURI);
            return unauthorizedResponse(exchange, "Token无效或已过期，请重新登录");
        }

        // 3. 从Token中获取管理员ID
        String adminId = jwtUtils.getUserIdFromToken(token);
        if (!StringUtils.hasText(adminId)) {
            log.warn("Token中无法获取管理员ID: {}", requestURI);
            return unauthorizedResponse(exchange, "Token格式错误，请重新登录");
        }

        log.info("✅ 管理员Token验证成功: adminId={}, uri={}", adminId, requestURI);

        // 4. 将管理员ID添加到请求头，传递给后端服务
        // 注意：管理员账户状态检查由后端服务负责（Gateway不访问数据库）
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Admin-Id", adminId)
                .header("X-Authenticated", "true")
                .header("X-User-Type", "admin")
                .build();

        log.info("✅ 已设置X-Admin-Id请求头: adminId={}, 准备转发到Admin Service", adminId);

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst(AuthCookieNames.ADMIN_TOKEN);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue();
        }

        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        String tokenParam = request.getQueryParams().getFirst("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        return null;
    }

    /**
     * 返回401未授权响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        
        // 检查响应是否已经提交
        if (response.isCommitted()) {
            log.warn("响应已提交，无法设置状态码: {}", exchange.getRequest().getURI().getPath());
            return Mono.empty();
        }
        
        // 在响应式环境中，需要先设置状态码，再设置响应头
        // 使用 Mono.fromRunnable 确保状态码在响应头之前设置
        return Mono.fromRunnable(() -> {
            try {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                log.error("设置状态码失败: {}", e.getMessage(), e);
            }
        })
        .then(Mono.fromRunnable(() -> {
            // 设置响应头
            try {
                if (!response.getHeaders().containsKey("Content-Type")) {
                    response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                }
            } catch (UnsupportedOperationException e) {
                log.debug("无法设置Content-Type响应头: {}", e.getMessage());
            }
        }))
        .then(Mono.defer(() -> {
            String body = String.format("{\"success\":false,\"errorMsg\":\"%s\"}", message);
            return response.writeWith(
                    Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8)))
            );
        }));
    }

    @Override
    public int getOrder() {
        // 设置优先级，在JwtAuthenticationFilter之后执行（因为管理员路径更具体）
        return -99;
    }
}

