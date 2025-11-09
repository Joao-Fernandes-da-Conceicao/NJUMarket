package com.njumarket.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * CORS过滤器
 * 处理CORS请求，避免与服务端CORS配置冲突
 * 对于WebSocket路径，完全移除Gateway添加的CORS头，由服务端处理
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)  // 在全局CORS配置之后执行，移除Gateway添加的CORS头
public class CorsFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestURI = exchange.getRequest().getURI().getPath();
        
        // WebSocket路径的CORS由服务端处理，Gateway完全移除自己添加的CORS头
        // 匹配所有WebSocket相关路径：/api/ws、/api/ws/、/api/ws/info等
        if (requestURI.startsWith("/api/ws")) {
            // 使用响应装饰器完全移除Gateway添加的所有CORS头
            ServerHttpResponse originalResponse = exchange.getResponse();
            ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(originalResponse) {
                private boolean corsHeadersRemoved = false;
                
                private void removeAllGatewayCorsHeaders(HttpHeaders headers) {
                    if (corsHeadersRemoved) {
                        return; // 避免重复处理
                    }
                    
                    // 完全移除所有Gateway可能添加的CORS头
                    // 服务端会自己添加正确的CORS头
                    java.util.List<String> removedHeaders = new java.util.ArrayList<>();
                    
                    // 先处理Vary头（如果包含CORS相关的值，需要清理）
                    if (headers.containsKey(HttpHeaders.VARY)) {
                        java.util.List<String> varyValues = headers.get(HttpHeaders.VARY);
                        if (varyValues != null) {
                            java.util.List<String> filteredVary = new java.util.ArrayList<>();
                            for (String vary : varyValues) {
                                // 保留非CORS相关的Vary值，移除CORS相关的
                                if (!vary.equalsIgnoreCase("Origin") && 
                                    !vary.equalsIgnoreCase("Access-Control-Request-Method") &&
                                    !vary.equalsIgnoreCase("Access-Control-Request-Headers")) {
                                    filteredVary.add(vary);
                                }
                            }
                            if (filteredVary.isEmpty()) {
                                headers.remove(HttpHeaders.VARY);
                                removedHeaders.add(HttpHeaders.VARY + "=removed(all CORS-related)");
                            } else {
                                headers.put(HttpHeaders.VARY, filteredVary);
                                removedHeaders.add(HttpHeaders.VARY + "=filtered(" + filteredVary + ")");
                            }
                        }
                    }
                    
                    // 移除所有CORS相关头
                    String[] corsHeaderNames = {
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        HttpHeaders.ACCESS_CONTROL_MAX_AGE
                    };
                    
                    for (String headerName : corsHeaderNames) {
                        if (headers.containsKey(headerName)) {
                            java.util.List<String> values = headers.get(headerName);
                            // 完全移除该头（包括所有值）
                            headers.remove(headerName);
                            removedHeaders.add(headerName + "=" + (values != null ? values.toString() : "null"));
                        }
                    }
                    
                    if (!removedHeaders.isEmpty()) {
                        corsHeadersRemoved = true;
                        log.debug("已移除Gateway添加的所有CORS头，让服务端处理: 路径={}, 移除的头={}", 
                            requestURI, String.join(", ", removedHeaders));
                    }
                }
                
                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = super.getHeaders();
                    // 在访问响应头时移除所有Gateway添加的CORS头
                    removeAllGatewayCorsHeaders(headers);
                    return headers;
                }
                
                @Override
                public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                    // 在写入响应体之前，移除所有Gateway添加的CORS头
                    HttpHeaders headers = getDelegate().getHeaders();
                    removeAllGatewayCorsHeaders(headers);
                    
                    // 使用 doOnSubscribe 确保在响应写入时移除CORS头
                    return super.writeWith(body)
                        .doOnSubscribe(subscription -> {
                            // 在订阅时再次移除CORS头
                            removeAllGatewayCorsHeaders(headers);
                        })
                        .doOnSuccess(aVoid -> {
                            // 在成功时再次移除CORS头（确保在响应提交前移除）
                            removeAllGatewayCorsHeaders(headers);
                        });
                }
            };
            
            return chain.filter(exchange.mutate().response(responseDecorator).build());
        }
        
        // 对于其他请求，使用Gateway的全局CORS配置
        return chain.filter(exchange);
    }
}

