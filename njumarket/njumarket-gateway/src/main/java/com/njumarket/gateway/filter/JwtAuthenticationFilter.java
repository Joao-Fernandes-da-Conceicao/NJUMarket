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

        // 只处理用户相关路径（前端直接调用 /api/user/**、/api/contact/** 和 /api/ws/**）
        // ✅ 添加WebSocket路径，因为WebSocket连接也需要JWT验证和X-User-Id传递
        if (!requestURI.startsWith("/api/user/") && 
            !requestURI.startsWith("/api/contact/") &&
            !requestURI.startsWith("/api/ws/")) {
            return chain.filter(exchange);
        }

        // 排除认证相关接口（这些接口不需要JWT验证）
        if (isAuthEndpoint(requestURI)) {
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

        // 3. 从Token中获取用户ID
        String userId = jwtUtils.getUserIdFromToken(token);
        if (!StringUtils.hasText(userId)) {
            log.warn("Token中无法获取用户ID: {}", requestURI);
            return unauthorizedResponse(exchange, "Token格式错误，请重新登录");
        }

        // 调试：打印userId的详细信息
        log.info("从Token解码得到的userId: [{}], 长度={}, 字符数组={}", 
            userId, userId.length(), java.util.Arrays.toString(userId.toCharArray()));

        // 4. 验证Redis中的Token（检查是否被撤销或登出）- 响应式方式
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + userId;
        
        log.info("开始查询Redis中的Token: userId=[{}], tokenKey=[{}], requestToken长度={}, requestToken前10字符={}...", 
            userId, tokenKey, token.length(), token.length() > 10 ? token.substring(0, 10) : token);
        
        // 直接读取Token值（简化逻辑，避免hasKey和get的时序问题）
        return reactiveStringRedisTemplate.opsForValue().get(tokenKey)
                .doOnSubscribe(subscription -> {
                    log.info("开始订阅Redis查询: userId={}, tokenKey={}", userId, tokenKey);
                })
                .doOnNext(cachedToken -> {
                    log.info("从Redis读取到Token: userId={}, tokenKey={}, cachedToken长度={}, cachedToken前10字符={}...", 
                        userId, tokenKey, 
                        cachedToken != null ? cachedToken.length() : 0,
                        cachedToken != null && cachedToken.length() > 10 ? cachedToken.substring(0, 10) : (cachedToken != null ? cachedToken : "null"));
                })
                .doOnError(error -> {
                    log.error("Redis读取Token时发生错误: userId={}, tokenKey={}, error={}", userId, tokenKey, error.getMessage(), error);
                })
                .defaultIfEmpty("")  // 如果key不存在，返回空字符串，避免触发switchIfEmpty
                .flatMap(cachedToken -> {
                    // 如果Redis中没有Token值（空字符串），说明key不存在或用户已登出
                    if (cachedToken == null || cachedToken.trim().isEmpty()) {
                        log.warn("Token已被撤销或用户已登出（Redis中不存在该key或值为空）: userId={}, uri={}, tokenKey={}", 
                            userId, requestURI, tokenKey);
                        
                        // 使用 hasKey 再次验证（用于调试）
                        return reactiveStringRedisTemplate.hasKey(tokenKey)
                                .flatMap(exists -> {
                                    if (Boolean.TRUE.equals(exists)) {
                                        log.error("异常：hasKey返回true但get()返回空: userId={}, tokenKey={}", userId, tokenKey);
                                    } else {
                                        log.warn("确认：hasKey返回false，key确实不存在: userId={}, tokenKey={}", userId, tokenKey);
                                    }
                                    return unauthorizedResponse(exchange, "Token已失效，请重新登录");
                                });
                    }

                    // 5. 验证Redis中的Token是否与请求中的Token一致（防止Token被替换）
                    if (!token.equals(cachedToken)) {
                        log.warn("Token不匹配（可能在其他设备登录）: userId={}, uri={}, requestToken长度={}, cachedToken长度={}, requestToken前10字符={}..., cachedToken前10字符={}...", 
                            userId, requestURI, token.length(), cachedToken.length(),
                            token.length() > 10 ? token.substring(0, 10) : token,
                            cachedToken.length() > 10 ? cachedToken.substring(0, 10) : cachedToken);
                        return unauthorizedResponse(exchange, "Token已失效，请重新登录");
                    }

                    // 6. 将用户ID添加到请求头，传递给后端服务
                    // 确保使用正确的userId（从外部作用域捕获，应该是正确的）
                    final String finalUserId = userId; // 使用final变量确保值不被修改
                    
                    // 使用finalUserId记录日志，确保显示正确的值
                    log.info("Token验证成功: userId=[{}], tokenKey=[{}], userId长度={}, tokenKey长度={}, finalUserId=[{}]", 
                        finalUserId, tokenKey, finalUserId.length(), tokenKey.length(), finalUserId);
                    
                    log.info("设置请求头X-User-Id: [{}], 长度={}", finalUserId, finalUserId.length());
                    
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", finalUserId)
                            .header("X-Authenticated", "true")
                            .build();
                    
                    log.info("请求头已设置，准备转发到后端服务: X-User-Id=[{}]", finalUserId);

                    // 直接返回 chain.filter 的结果，不需要额外包装
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(e -> {
                    log.error("Redis查询Token失败: userId={}, uri={}, tokenKey={}, error={}, errorType={}", 
                        userId, requestURI, tokenKey, e.getMessage(), e.getClass().getName(), e);
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

