package com.njumarket.commodity.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Message Service Feign Client
 * 用于Commodity Service调用Message Service的WebSocket推送功能
 */
@FeignClient(name = "njumarket-service-message", path = "/api/internal")
public interface MessageClient {
    
    /**
     * WebSocket推送消息
     */
    @PostMapping("/websocket/push")
    Result pushMessage(@RequestParam String receiverId,
                      @RequestParam String messageType,
                      @RequestBody Map<String, Object> messageData);
}

