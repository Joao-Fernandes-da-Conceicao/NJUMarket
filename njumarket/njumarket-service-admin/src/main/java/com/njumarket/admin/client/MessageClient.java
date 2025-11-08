package com.njumarket.admin.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "njumarket-service-message", path = "/api/internal")
public interface MessageClient {

    @GetMapping("/conversation/{conversationId}")
    Result getConversationById(@PathVariable String conversationId);

    @GetMapping("/conversations")
    Result listConversations(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer size,
                            @RequestParam(required = false) String keyword);

    @PutMapping("/conversation/{conversationId}/full")
    Result updateConversationFull(@PathVariable String conversationId,
                                  @RequestBody Map<String, Object> payload);

    @DeleteMapping("/conversation/{conversationId}")
    Result deleteConversation(@PathVariable String conversationId);

    @GetMapping("/message/{messageId}")
    Result getMessageById(@PathVariable String messageId);

    @GetMapping("/messages")
    Result listMessages(@RequestParam String conversationId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size);

    @PutMapping("/message/{messageId}/full")
    Result updateMessageFull(@PathVariable String messageId,
                            @RequestBody Map<String, Object> payload);

    @DeleteMapping("/message/{messageId}")
    Result deleteMessage(@PathVariable String messageId);
}

