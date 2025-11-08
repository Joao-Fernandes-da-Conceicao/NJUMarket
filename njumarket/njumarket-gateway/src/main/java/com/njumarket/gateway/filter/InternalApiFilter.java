package com.njumarket.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 内部API访问控制Filter
 * 阻止外部用户通过Gateway访问内部API（/api/internal/**）
 * 内部API只能通过服务间直接调用（不经过Gateway）
 */
@Slf4j
@Component
public class InternalApiFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestURI = exchange.getRequest().getURI().getPath();

        // 检查是否为内部API路径
        if (requestURI.contains("/api/internal/")) {
            log.warn("外部访问内部API被阻止: {}", requestURI);
            return forbiddenResponse(exchange, "内部API不允许外部访问");
        }

        // 允许其他请求通过
        return chain.filter(exchange);
    }

    /**
     * 返回403禁止访问响应
     */
    private Mono<Void> forbiddenResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        
        // 检查响应是否已经提交
        if (response.isCommitted()) {
            return Mono.empty();
        }
        
        // 设置状态码和响应头
        response.setStatusCode(HttpStatus.FORBIDDEN);
        
        // 使用 add 而不是 set，避免在只读响应头时抛出异常
        try {
            response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        } catch (UnsupportedOperationException e) {
            // 如果响应头是只读的，记录日志但不抛出异常
            log.debug("无法设置Content-Type响应头: {}", e.getMessage());
        }
        
        String body = String.format("{\"success\":false,\"errorMsg\":\"%s\"}", message);
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8)))
        );
    }

    @Override
    public int getOrder() {
        // 设置最高优先级，在其他Filter之前执行
        return -200;
    }
}

