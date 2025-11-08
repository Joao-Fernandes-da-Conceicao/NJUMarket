package com.njumarket.message.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 信息控制器
 * 提供 WebSocket 连接状态和调试信息
 * 处理 SockJS 的 /info 请求
 * 
 * 注意：SockJS 期望返回纯 JSON，而不是 Result 包装的响应
 * 因此这里直接返回 Map，Spring 会自动序列化为 JSON
 */
@Slf4j
@RestController
@RequestMapping("/ws")
public class WebSocketInfoController {

    /**
     * 获取 WebSocket 信息（SockJS info 端点）
     * 用于调试和监控 WebSocket 连接状态
     * 
     * SockJS 协议要求：
     * 1. 返回纯 JSON（不是 Result 包装）
     * 2. 必须包含 websocket、origins、cookie_needed、entropy 字段
     * 3. 可以包含 transports 字段（可选）
     * 
     * @param t 时间戳（SockJS 自动添加）
     * @return WebSocket 服务器信息（纯 JSON）
     */
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getWebSocketInfo(@RequestParam(required = false) String t) {
        try {
            log.info("收到 WebSocket info 请求: t={}", t);
            
            Map<String, Object> info = new HashMap<>();
            
            // ✅ SockJS 标准响应字段（必需）
            info.put("websocket", true);  // 支持 WebSocket
            info.put("origins", new String[]{"*:*"});  // 允许所有来源
            info.put("cookie_needed", false);  // 不需要 Cookie
            info.put("entropy", System.currentTimeMillis());  // 随机熵值（用于会话ID生成）
            
            // ✅ 支持的传输方式（SockJS 会按顺序尝试这些方式）
            info.put("transports", new String[]{
                "websocket",           // WebSocket（首选，性能最好）
                "xhr-streaming",       // XHR 流式传输（降级方案1）
                "xhr-polling",         // XHR 轮询（降级方案2）
                "iframe-eventsource",  // iframe EventSource（降级方案3）
                "iframe-htmlfile",     // iframe HTMLFile（降级方案4，IE8-9）
                "iframe-xhr-polling"   // iframe XHR 轮询（降级方案5，IE8-9）
            });
            
            // ✅ 扩展调试信息（不影响 SockJS 功能，仅用于调试）
            Map<String, Object> debug = new HashMap<>();
            debug.put("server_name", "NJUMarket Message Service");
            debug.put("server_version", "2.0");
            debug.put("timestamp", LocalDateTime.now().toString());
            debug.put("status", "running");
            debug.put("endpoint", "/ws");
            debug.put("stomp_prefix", "/app");
            debug.put("user_destination_prefix", "/user");
            debug.put("broker_prefixes", new String[]{"/queue", "/topic"});
            info.put("debug", debug);
            
            log.info("返回 WebSocket info: websocket={}, transports={}, entropy={}", 
                info.get("websocket"), info.get("transports"), info.get("entropy"));
            
            return info;
            
        } catch (Exception e) {
            log.error("获取 WebSocket 信息失败: {}", e.getMessage(), e);
            
            // 即使出错，也返回基本的 SockJS 响应，避免连接失败
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("websocket", true);
            errorInfo.put("origins", new String[]{"*:*"});
            errorInfo.put("cookie_needed", false);
            errorInfo.put("entropy", System.currentTimeMillis());
            errorInfo.put("transports", new String[]{"websocket", "xhr-streaming", "xhr-polling"});
            errorInfo.put("error", "获取调试信息失败: " + e.getMessage());
            return errorInfo;
        }
    }
}

