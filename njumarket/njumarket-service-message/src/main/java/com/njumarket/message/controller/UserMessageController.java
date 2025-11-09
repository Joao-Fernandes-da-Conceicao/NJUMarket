package com.njumarket.message.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.message.dto.MessageDTO;
import com.njumarket.message.service.MessageService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户消息", description = "用户消息管理功能")
@RestController
@RequestMapping("/api/user/message")
@RequiredArgsConstructor
public class UserMessageController {

    private final MessageService messageService;

    @Operation(summary = "发送消息", description = "发送消息给其他用户")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "发送成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "接收用户不存在")
    })
    @PostMapping("/send")
    public Result sendMessage(@Valid @RequestBody MessageDTO messageDTO) {
        return messageService.sendMessage(messageDTO);
    }

    @Operation(summary = "获取消息列表", description = "获取当前用户的对话列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/conversations")
    public Result getConversations() {
        return messageService.getConversations();
    }

    @Operation(summary = "获取聊天记录", description = "获取与指定用户的聊天记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/chat/{userId}")
    public Result getChatHistory(@Parameter(description = "用户ID", required = true) @PathVariable String userId,
                               @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
                               @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        return messageService.getChatHistory(userId, page, size);
    }

    @Operation(summary = "标记消息为已读", description = "将指定消息标记为已读")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "标记成功"),
        @ApiResponse(responseCode = "404", description = "消息不存在")
    })
    @PostMapping("/{messageId}/read")
    public Result markAsRead(@Parameter(description = "消息ID", required = true) @PathVariable String messageId) {
        return messageService.markAsRead(messageId);
    }

    @Operation(summary = "批量标记消息为已读", description = "批量将消息标记为已读")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "标记成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/batch-read")
    public Result batchMarkAsRead(@Parameter(description = "消息ID数组", required = true) @RequestBody String[] messageIds) {
        return messageService.batchMarkAsRead(messageIds);
    }

    @Operation(summary = "删除消息", description = "删除指定的消息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "消息不存在")
    })
    @DeleteMapping("/{messageId}")
    public Result deleteMessage(@Parameter(description = "消息ID", required = true) @PathVariable String messageId) {
        return messageService.deleteMessage(messageId);
    }

    @Operation(summary = "获取未读消息数量", description = "获取当前用户的未读消息数量")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/unread-count")
    public Result getUnreadCount() {
        return messageService.getUnreadCount();
    }

    @Operation(summary = "搜索消息", description = "搜索消息内容")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping("/search")
    public Result searchMessages(@Parameter(description = "搜索关键词", required = true) @RequestParam String keyword,
                               @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
                               @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size) {
        return messageService.searchMessages(keyword, page, size);
    }

    @Operation(summary = "请求查看联系方式", description = "请求查看对方的联系方式")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "请求成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @PostMapping("/request-contact/{userId}")
    public Result requestContact(@Parameter(description = "用户ID", required = true) @PathVariable String userId) {
        return messageService.requestContact(userId);
    }

    @Operation(summary = "授权查看联系方式", description = "授权对方查看联系方式")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "授权成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @PostMapping("/grant-contact/{userId}")
    public Result grantContact(@Parameter(description = "用户ID", required = true) @PathVariable String userId) {
        return messageService.grantContact(userId);
    }
}

