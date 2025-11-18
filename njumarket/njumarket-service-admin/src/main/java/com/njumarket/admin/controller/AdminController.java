package com.njumarket.admin.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.admin.dto.AdminLoginDTO;
import com.njumarket.admin.entity.Admin;
import com.njumarket.admin.service.AdminService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 */
@Tag(name = "管理员管理", description = "管理员登录和管理相关接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "管理员登录", description = "管理员使用用户名和密码登录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public Result login(@Valid @RequestBody AdminLoginDTO loginDTO, HttpSession session) {
        return adminService.login(loginDTO, session);
    }

    @Operation(summary = "管理员登出", description = "管理员登出系统")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登出成功")
    })
    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        return adminService.logout(session);
    }

    @Operation(summary = "获取当前管理员信息", description = "获取当前登录管理员的详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "管理员未登录")
    })
    @GetMapping("/me")
    public Result getCurrentAdmin() {
        return adminService.getCurrentAdmin();
    }

    @Operation(summary = "创建管理员", description = "创建新的管理员账号")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "409", description = "用户名已存在")
    })
    @PostMapping("/create")
    @PreAuthorize("hasRole('SYSTEM')") // 只有system管理员可以创建管理员
    public Result createAdmin(@RequestBody Admin admin) {
        return adminService.createAdmin(admin);
    }

    @Operation(summary = "更新管理员信息", description = "更新管理员的基本信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @PutMapping("/{adminId}")
    @PreAuthorize("hasRole('SYSTEM') or #adminId == authentication.principal.adminId") // system可以更新所有管理员，普通管理员只能更新自己的
    public Result updateAdmin(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @RequestBody Admin admin) {
        return adminService.updateAdmin(adminId, admin);
    }

    @Operation(summary = "完整更新管理员信息", description = "只有system权限的管理员可以更新所有非客观字段（包括密码）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @PutMapping("/{adminId}/full")
    @PreAuthorize("hasRole('SYSTEM')") // 只有system管理员可以完整更新管理员
    public Result updateAdminFull(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @RequestBody java.util.Map<String, Object> payload) {
        return adminService.updateAdminFull(adminId, payload);
    }

    @Operation(summary = "删除管理员", description = "删除指定的管理员账号")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在"),
        @ApiResponse(responseCode = "403", description = "不能删除系统管理员")
    })
    @DeleteMapping("/{adminId}")
    @PreAuthorize("hasRole('SYSTEM')") // 只有system管理员可以删除管理员
    public Result deleteAdmin(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId) {
        return adminService.deleteAdmin(adminId);
    }

    @Operation(summary = "获取管理员列表", description = "分页获取管理员列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/list")
    @PreAuthorize("hasRole('SYSTEM')") // 只有system管理员可以查看管理员列表
    public Result getAdminList(
        @Parameter(description = "页码")
        @RequestParam(defaultValue = "1") Integer page,
        @Parameter(description = "每页大小")
        @RequestParam(defaultValue = "10") Integer size,
        @Parameter(description = "搜索关键词")
        @RequestParam(required = false) String keyword,
        @Parameter(description = "账户状态筛选")
        @RequestParam(required = false) String accountStatus,
        @Parameter(description = "排序字段")
        @RequestParam(required = false) String sortProp,
        @Parameter(description = "排序方向")
        @RequestParam(required = false) String sortOrder) {
        return adminService.getAdminList(page, size, keyword, accountStatus, sortProp, sortOrder);
    }

    @Operation(summary = "获取管理员详情", description = "根据ID获取管理员详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @GetMapping("/{adminId}")
    @PreAuthorize("hasRole('SYSTEM') or #adminId == authentication.principal.adminId") // system可以查看所有管理员，普通管理员只能查看自己的
    public Result getAdminById(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId) {
        return adminService.getAdminById(adminId);
    }

    @Operation(summary = "更新管理员状态", description = "更新管理员的账户状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @PutMapping("/{adminId}/status")
    @PreAuthorize("hasRole('SYSTEM')") // 只有system管理员可以更新管理员状态
    public Result updateAdminStatus(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @Parameter(description = "账户状态", required = true)
        @RequestParam String status) {
        return adminService.updateAdminStatus(adminId, status);
    }

    @Operation(summary = "重置管理员密码", description = "重置指定管理员的密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重置成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @PutMapping("/{adminId}/reset-password")
    @PreAuthorize("hasRole('SYSTEM')") // 只有system管理员可以重置密码
    public Result resetPassword(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @Parameter(description = "新密码", required = true)
        @RequestParam String newPassword) {
        return adminService.resetPassword(adminId, newPassword);
    }

    @Operation(summary = "修改密码", description = "管理员修改自己的密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "400", description = "原密码错误")
    })
    @PutMapping("/change-password")
    @PreAuthorize("#adminId == authentication.principal.adminId") // 只能修改自己的密码
    public Result changePassword(
        @Parameter(description = "管理员ID", required = true)
        @RequestParam String adminId,
        @Parameter(description = "原密码", required = true)
        @RequestParam String oldPassword,
        @Parameter(description = "新密码", required = true)
        @RequestParam String newPassword) {
        return adminService.changePassword(adminId, oldPassword, newPassword);
    }

    @Operation(summary = "获取管理员统计信息", description = "获取管理员的统计信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('SYSTEM')") // 只有system管理员可以查看统计信息
    public Result getAdminStatistics() {
        return adminService.getAdminStatistics();
    }

    @Operation(summary = "检查管理员权限", description = "检查管理员是否具有指定权限")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "检查成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @GetMapping("/{adminId}/permission")
    public Result checkPermission(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @Parameter(description = "权限名称", required = true)
        @RequestParam String permission) {
        return adminService.checkPermission(adminId, permission);
    }

    @Operation(summary = "更新管理员权限", description = "更新管理员的权限列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @PutMapping("/{adminId}/permissions")
    @PreAuthorize("hasRole('SYSTEM')") // 只有system管理员可以更新权限
    public Result updatePermissions(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @RequestBody List<String> permissions) {
        return adminService.updatePermissions(adminId, permissions);
    }

    // ===================== 管理端最小CRUD：用户 =====================
    @GetMapping("/users")
    public Result listUsers(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer size,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String accountStatus,
                            @RequestParam(required = false) String sortProp,
                            @RequestParam(required = false) String sortOrder) {
        return adminService.listUsers(page, size, keyword, accountStatus, sortProp, sortOrder);
    }

    @GetMapping("/users/{userId}")
    public Result getUser(@PathVariable String userId) {
        return adminService.getUserById(userId);
    }

    @PutMapping("/users/{userId}/status")
    public Result updateUserStatus(@PathVariable String userId,
                                   @RequestParam String status) {
        return adminService.updateUserStatus(userId, status);
    }

    @PutMapping("/users/{userId}")
    public Result updateUserBasic(@PathVariable String userId,
                                  @RequestParam(required = false) String nickname,
                                  @RequestParam(required = false) String phone,
                                  @RequestParam(required = false) String email) {
        return adminService.updateUserBasic(userId, nickname, phone, email);
    }

    @DeleteMapping("/users/{userId}")
    public Result deleteUser(@PathVariable String userId) {
        return adminService.deleteUser(userId);
    }

    @PutMapping("/users/{userId}/full")
    public Result updateUserFull(@PathVariable String userId, @RequestBody java.util.Map<String, Object> payload) {
        return adminService.updateUserFull(userId, payload);
    }

    // ===================== 管理端最小CRUD：用户地址 =====================
    @GetMapping("/users/{userId}/addresses")
    public Result listUserAddresses(@PathVariable String userId) {
        return adminService.listUserAddresses(userId);
    }

    @PostMapping("/users/{userId}/addresses")
    public Result createUserAddress(@PathVariable String userId, @RequestBody Map<String, Object> payload) {
        return adminService.createUserAddress(userId, payload);
    }

    @PutMapping("/users/{userId}/addresses/{addressId}")
    public Result updateUserAddress(@PathVariable String userId,
                                    @PathVariable String addressId,
                                    @RequestBody Map<String, Object> payload) {
        return adminService.updateUserAddress(userId, addressId, payload);
    }

    @DeleteMapping("/users/{userId}/addresses/{addressId}")
    public Result deleteUserAddress(@PathVariable String userId,
                                    @PathVariable String addressId) {
        return adminService.deleteUserAddress(userId, addressId);
    }

    @PutMapping("/users/{userId}/addresses/{addressId}/default")
    public Result setUserAddressDefault(@PathVariable String userId,
                                        @PathVariable String addressId) {
        return adminService.setUserAddressDefault(userId, addressId);
    }

    // ===================== 管理端最小CRUD：商品 =====================
    @GetMapping("/commodities")
    public Result listCommodities(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) String conditionLevel,
                                  @RequestParam(required = false, name = "status") String commodityStatus,
                                  @RequestParam(required = false) String sellerVisibility,
                                  @RequestParam(required = false) String buyerVisibility,
                                  @RequestParam(required = false) String sortProp,
                                  @RequestParam(required = false) String sortOrder) {
        return adminService.listCommodities(page, size, keyword, category, conditionLevel, commodityStatus, sellerVisibility, buyerVisibility, sortProp, sortOrder);
    }

    @GetMapping("/commodities/{commodityId}")
    public Result getCommodity(@PathVariable String commodityId) {
        return adminService.getCommodityById(commodityId);
    }

    @PutMapping("/commodities/{commodityId}/status")
    public Result updateCommodityStatus(@PathVariable String commodityId,
                                        @RequestParam String status) {
        return adminService.updateCommodityStatus(commodityId, status);
    }

    @DeleteMapping("/commodities/{commodityId}")
    public Result deleteCommodity(@PathVariable String commodityId) {
        return adminService.deleteCommodity(commodityId);
    }

    @PutMapping("/commodities/{commodityId}/full")
    public Result updateCommodityFull(@PathVariable String commodityId, @RequestBody java.util.Map<String, Object> payload) {
        return adminService.updateCommodityFull(commodityId, payload);
    }

    // ===================== 管理端最小CRUD：订单 =====================
    @GetMapping("/orders")
    public Result listOrders(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String sellerVisibility,
                             @RequestParam(required = false) String buyerVisibility,
                             @RequestParam(required = false) String sortProp,
                             @RequestParam(required = false) String sortOrder) {
        return adminService.listOrders(page, size, keyword, status, sellerVisibility, buyerVisibility, sortProp, sortOrder);
    }

    @GetMapping("/orders/{orderId}")
    public Result getOrder(@PathVariable String orderId) {
        return adminService.getOrderById(orderId);
    }

    @PutMapping("/orders/{orderId}")
    public Result updateOrder(@PathVariable String orderId,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String trackingNumber,
                              @RequestParam(required = false) String remark) {
        return adminService.updateOrderFields(orderId, status, trackingNumber, remark);
    }

    @PutMapping("/orders/{orderId}/full")
    public Result updateOrderFull(@PathVariable String orderId,
                                  @RequestBody java.util.Map<String, Object> payload) {
        return adminService.updateOrderFull(orderId, payload);
    }

    @DeleteMapping("/orders/{orderId}")
    public Result deleteOrder(@PathVariable String orderId) {
        return adminService.deleteOrder(orderId);
    }

    // ===================== 管理端最小CRUD：会话/消息 =====================
    @GetMapping("/conversations")
    public Result listConversations(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    @RequestParam(required = false) String keyword) {
        return adminService.listConversations(page, size, keyword);
    }

    @GetMapping("/conversations/{conversationId}")
    public Result getConversationById(@PathVariable String conversationId) {
        return adminService.getConversationById(conversationId);
    }

    @PutMapping("/conversations/{conversationId}/full")
    public Result updateConversationFull(@PathVariable String conversationId,
                                         @RequestBody java.util.Map<String, Object> payload) {
        return adminService.updateConversationFull(conversationId, payload);
    }

    @DeleteMapping("/conversations/{conversationId}")
    public Result deleteConversation(@PathVariable String conversationId) {
        return adminService.deleteConversation(conversationId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Result listMessages(@PathVariable String conversationId,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size) {
        return adminService.listMessages(conversationId, page, size);
    }

    @GetMapping("/messages/{messageId}")
    public Result getMessageById(@PathVariable String messageId) {
        return adminService.getMessageById(messageId);
    }

    @PutMapping("/messages/{messageId}/full")
    public Result updateMessageFull(@PathVariable String messageId,
                                    @RequestBody java.util.Map<String, Object> payload) {
        return adminService.updateMessageFull(messageId, payload);
    }

    @DeleteMapping("/messages/{messageId}")
    public Result deleteMessage(@PathVariable String messageId) {
        return adminService.deleteMessage(messageId);
    }
}

