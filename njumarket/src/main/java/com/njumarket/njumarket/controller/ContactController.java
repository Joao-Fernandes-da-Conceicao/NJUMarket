package com.njumarket.njumarket.controller;

//import com.njumarket.njumarket.dto.ConversationDTO;
//import com.njumarket.njumarket.dto.MessageDTO;
import com.njumarket.njumarket.dto.SendMessageRequest;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户联系", description = "买家与卖家之间的消息联系功能")
@RestController
@RequestMapping("/api/contact")
public class ContactController {
    
    @Autowired
    private ContactService contactService;
    
    @Operation(summary = "发送消息", description = "向对方发送消息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "发送成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "对话不存在")
    })
    @PostMapping("/send")
    public Result sendMessage(@RequestAttribute("userId") String userId,
                             @RequestBody SendMessageRequest request) {
        return contactService.sendMessage(userId, request);
    }
    
    @Operation(summary = "获取对话列表", description = "获取当前用户的所有对话列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/conversations")
    public Result getConversations(
            @RequestAttribute("userId") String userId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int size) {
        return contactService.getConversations(userId, page - 1, size);
    }
    
    @Operation(summary = "获取对话详情", description = "获取对话详情及消息历史记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "对话不存在")
    })
    @GetMapping("/conversations/{conversationId}")
    public Result getConversationDetail(
            @RequestAttribute("userId") String userId,
            @Parameter(description = "对话ID", required = true) @PathVariable String conversationId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "50") @RequestParam(defaultValue = "50") int size) {
        return contactService.getConversationDetail(userId, conversationId, page - 1, size);
    }
    
    @Operation(summary = "创建或获取对话", description = "创建新的对话或获取现有对话（基于用户对，确保唯一性）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/conversations/create")
    public Result createConversation(
            @RequestAttribute("userId") String userId,
            @Parameter(description = "对方用户ID", required = true) @RequestParam String otherUserId) {
        return contactService.getOrCreateConversation(userId, otherUserId);
    }
    
    @Operation(summary = "标记对话为已读", description = "将对话中的所有消息标记为已读")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "标记成功"),
        @ApiResponse(responseCode = "404", description = "对话不存在")
    })
    @PostMapping("/conversations/{conversationId}/read")
    public Result markAsRead(@RequestAttribute("userId") String userId,
                            @Parameter(description = "对话ID", required = true) @PathVariable String conversationId) {
        return contactService.markConversationAsRead(userId, conversationId);
    }
    
    @Operation(summary = "获取未读消息总数", description = "获取当前用户的未读消息总数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/unread-count")
    public Result getUnreadCount(@RequestAttribute("userId") String userId) {
        return contactService.getUnreadCount(userId);
    }
    
    @Operation(summary = "删除对话", description = "删除指定的对话")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "对话不存在")
    })
    @DeleteMapping("/conversations/{conversationId}")
    public Result deleteConversation(@RequestAttribute("userId") String userId,
                                    @Parameter(description = "对话ID", required = true) @PathVariable String conversationId) {
        return contactService.deleteConversation(userId, conversationId);
    }
    
    @Operation(summary = "删除消息", description = "删除指定的消息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "消息不存在")
    })
    @DeleteMapping("/messages/{messageId}")
    public Result deleteMessage(@RequestAttribute("userId") String userId,
                               @Parameter(description = "消息ID", required = true) @PathVariable String messageId) {
        return contactService.deleteMessage(userId, messageId);
    }
    
    @Operation(summary = "搜索消息", description = "在指定对话中搜索消息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "404", description = "对话不存在")
    })
    @GetMapping("/conversations/{conversationId}/search")
    public Result searchMessages(
            @RequestAttribute("userId") String userId,
            @Parameter(description = "对话ID", required = true) @PathVariable String conversationId,
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int size) {
        return contactService.searchMessages(userId, conversationId, keyword, page - 1, size);
    }
    
    @Operation(summary = "获取与特定用户的对话", description = "获取与指定用户之间的对话")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "对话不存在")
    })
    @GetMapping("/conversations/with/{otherUserId}")
    public Result getConversationWithUser(
            @RequestAttribute("userId") String userId,
            @Parameter(description = "对方用户ID", required = true) @PathVariable String otherUserId) {
        return contactService.getConversationWithUser(userId, otherUserId);
    }
}
