package com.njumarket.gateway.filter;

import com.njumarket.njumarket.utils.JwtUtils;
import com.njumarket.njumarket.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
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
 * JWT认证Gateway Filter（响应式）
 * 在Gateway层统一处理JWT认证，验证Token有效性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestURI = request.getURI().getPath();

        // 只处理用户相关路径（前端直接调用 /api/user/**、/api/contact/**、/api/auth/** 和 /api/ws/**）
        // ✅ 添加WebSocket路径，因为WebSocket连接也需要JWT验证和X-User-Id传递
        // ✅ 添加 /api/auth/** 路径，因为地址管理等接口需要JWT验证
        if (!requestURI.startsWith("/api/user/") && 
            !requestURI.startsWith("/api/contact/") &&
            !requestURI.startsWith("/api/auth/") &&
            !requestURI.startsWith("/api/ws/")) {
            return chain.filter(exchange);
        }

        // 排除认证相关接口（这些接口不需要JWT验证）
        if (isAuthEndpoint(requestURI)) {
            return chain.filter(exchange);
        }
        
        // 排除 WebSocket info 端点（SockJS 需要这个端点来获取服务器信息，不需要 JWT）
        // SockJS 会在建立 WebSocket 连接前先请求 /info 端点
        if (requestURI.equals("/api/ws/info") || requestURI.equals("/api/ws/order/info")) {
            return chain.filter(exchange);
        }

        // 1. 从请求头获取Token
        String token = getTokenFromRequest(request);
        
        if (!StringUtils.hasText(token)) {
            log.warn("请求缺少Authorization头: {}", requestURI);
            return unauthorizedResponse(exchange, "用户未登录，请先登录");
        }

        // 2. 验证Token有效性（先验证JWT本身）
        if (!jwtUtils.validateToken(token)) {
            log.warn("Token验证失败（JWT过期）: {}", requestURI);
            return unauthorizedResponse(exchange, "Token无效或已过期，请重新登录");
        }

        // 3. 新版 Session Token（JWT 内是 sid，不是 userId）
        String sessionId = jwtUtils.getSessionIdFromToken(token);
        if (StringUtils.hasText(sessionId)) {
            String sessionKey = RedisConstants.SESSION_KEY + sessionId;
            return reactiveStringRedisTemplate.opsForHash().get(sessionKey, "userId")
                    .map(Object::toString)
                    .defaultIfEmpty("")
                    .flatMap(userId -> {
                        if (!StringUtils.hasText(userId)) {
                            log.warn("Session不存在或已过期: sessionId={}, uri={}", sessionId, requestURI);
                            return unauthorizedResponse(exchange, "Token已失效，请重新登录");
                        }
                        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + userId;
                        return reactiveStringRedisTemplate.opsForValue().get(tokenKey)
                                .defaultIfEmpty("")
                                .flatMap(currentSessionId -> {
                                    // 单设备策略：login:token:{userId} 必须指向当前 sid
                                    if (!sessionId.equals(currentSessionId)) {
                                        log.warn("Session不匹配（可能已在其他设备登录）: userId={}, uri={}, sid={}, redisSid={}",
                                                userId, requestURI, sessionId, currentSessionId);
                                        return unauthorizedResponse(exchange, "Token已失效，请重新登录");
                                    }
                                    ServerHttpRequest modifiedRequest = request.mutate()
                                            .header("X-User-Id", userId)
                                            .header("X-Authenticated", "true")
                                            .build();
                                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                                });
                    })
                    .onErrorResume(e -> {
                        log.error("Session模式Token验证失败: sessionId={}, uri={}, error={}", sessionId, requestURI, e.getMessage(), e);
                        return unauthorizedResponse(exchange, "Token验证失败，请重新登录");
                    });
        }

        // 4. 兼容旧版 Token（JWT 内含 userId；Redis 中存的是原 token 字符串）
        String userId = jwtUtils.getUserIdFromToken(token);
        if (!StringUtils.hasText(userId)) {
            log.warn("Token中无法获取用户ID或SessionID: {}", requestURI);
            return unauthorizedResponse(exchange, "Token格式错误，请重新登录");
        }
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + userId;
        return reactiveStringRedisTemplate.opsForValue().get(tokenKey)
                .defaultIfEmpty("")
                .flatMap(cachedToken -> {
                    if (!StringUtils.hasText(cachedToken) || !token.equals(cachedToken)) {
                        log.warn("旧版Token不匹配或已失效: userId={}, uri={}", userId, requestURI);
                        return unauthorizedResponse(exchange, "Token已失效，请重新登录");
                    }
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", userId)
                            .header("X-Authenticated", "true")
                            .build();
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(e -> {
                    log.error("旧版Token验证失败: userId={}, uri={}, error={}", userId, requestURI, e.getMessage(), e);
                    return unauthorizedResponse(exchange, "Token验证失败，请重新登录");
                });
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(ServerHttpRequest request) {
        // 1. 从Authorization头获取
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. 从查询参数获取（备用方案）
        String tokenParam = request.getQueryParams().getFirst("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        return null;
    }

    /**
     * 判断是否为认证端点（不需要JWT验证）
     */
    private boolean isAuthEndpoint(String requestURI) {
        return requestURI.equals("/api/user/auth/login") ||
               requestURI.equals("/api/user/auth/register") ||
               requestURI.equals("/api/user/auth/register-new") ||
               requestURI.equals("/api/user/auth/send-code") ||
               requestURI.equals("/api/user/auth/login-by-code") ||
               requestURI.equals("/api/user/auth/login-third-party") ||
               requestURI.equals("/api/user/auth/reset-password") ||
               requestURI.equals("/api/user/auth/refresh-token");
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
        
        // 在响应式环境中，必须同步设置状态码和响应头，然后写入响应体
        // 使用 Mono.defer 确保在订阅时才执行
        return Mono.defer(() -> {
            try {
                // 同步设置状态码（必须在写入响应体之前）
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                
                // 设置响应头
                try {
                    if (!response.getHeaders().containsKey("Content-Type")) {
                        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                    }
                } catch (UnsupportedOperationException e) {
                    log.debug("无法设置Content-Type响应头: {}", e.getMessage());
                }
                
                // 写入响应体
                String body = String.format("{\"success\":false,\"errorMsg\":\"%s\"}", message);
                return response.writeWith(
                        Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8)))
                );
            } catch (Exception e) {
                log.error("构建401响应失败: {}", e.getMessage(), e);
                // 如果设置失败，尝试直接返回空响应
                return Mono.empty();
            }
        });
    }

    @Override
    public int getOrder() {
        // 设置优先级，确保在其他Filter之前执行
        return -100;
    }
}

