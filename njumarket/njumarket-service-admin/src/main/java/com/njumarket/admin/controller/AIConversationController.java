package com.njumarket.admin.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.admin.service.AIConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 聊天会话管理控制器（管理端）
 */
@Tag(name = "AI Agent管理", description = "AI聊天会话管理相关接口")
@RestController
@RequestMapping("/api/admin/ai-conversations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SYSTEM','ADMINISTRATOR')")
public class AIConversationController {
    
    private final AIConversationService aiConversationService;
    
    @Operation(summary = "获取会话列表", description = "分页获取AI聊天会话列表，支持按用户ID、状态、关键词筛选")
    @GetMapping
    public Result getConversationList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortProp,
            @RequestParam(required = false) String sortOrder) {
        return aiConversationService.getConversationList(page, size, userId, status, keyword, sortProp, sortOrder);
    }
    
    @Operation(summary = "获取会话详情", description = "根据会话ID获取会话详情")
    @GetMapping("/{conversationId}")
    public Result getConversationDetail(@PathVariable String conversationId) {
        return aiConversationService.getConversationDetail(conversationId);
    }
    
    @Operation(summary = "删除会话", description = "软删除会话（将状态设置为DELETED）")
    @DeleteMapping("/{conversationId}")
    public Result deleteConversation(@PathVariable String conversationId) {
        return aiConversationService.deleteConversation(conversationId);
    }
    
    @Operation(summary = "批量删除会话", description = "批量软删除会话")
    @DeleteMapping("/batch")
    public Result batchDeleteConversations(@RequestBody List<String> conversationIds) {
        return aiConversationService.batchDeleteConversations(conversationIds);
    }
    
    @Operation(summary = "恢复会话", description = "恢复已删除的会话（将状态从DELETED改为ACTIVE）")
    @PostMapping("/{conversationId}/restore")
    public Result restoreConversation(@PathVariable String conversationId) {
        return aiConversationService.restoreConversation(conversationId);
    }
    
    @Operation(summary = "获取统计信息", description = "获取会话统计信息（总数、活跃数、已删除数）")
    @GetMapping("/statistics")
    public Result getConversationStatistics() {
        return aiConversationService.getConversationStatistics();
    }
}

