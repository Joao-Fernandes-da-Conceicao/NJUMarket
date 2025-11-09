package com.njumarket.admin.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.admin.dto.AdminLoginDTO;
// Admin 实体已迁移到 auth-service
import jakarta.servlet.http.HttpSession;

import java.util.List;

/**
 * 管理员服务接口
 */
public interface AdminService {
    
    /** 管理员登录 */
    Result login(AdminLoginDTO loginDTO, HttpSession session);
    /** 管理员登出 */
    Result logout(HttpSession session);
    /** 获取当前登录的管理员信息 */
    Result getCurrentAdmin();
    /** 创建管理员账号 */
    Result createAdmin(com.njumarket.admin.entity.Admin admin);
    /** 更新管理员信息 */
    Result updateAdmin(String adminId, com.njumarket.admin.entity.Admin admin);
    /** 完整更新管理员信息（包括所有非客观字段，只有system权限可用） */
    Result updateAdminFull(String adminId, java.util.Map<String, Object> payload);
    /** 删除管理员账号 */
    Result deleteAdmin(String adminId);
    /** 获取管理员列表（只有system权限可用） */
    Result getAdminList(Integer page, Integer size, String keyword, String accountStatus, String sortProp, String sortOrder);
    /** 根据ID获取管理员信息（只有system权限可用） */
    Result getAdminById(String adminId);
    /** 更新管理员状态 */
    Result updateAdminStatus(String adminId, String status);
    /** 重置管理员密码 */
    Result resetPassword(String adminId, String newPassword);
    /** 修改密码 */
    Result changePassword(String adminId, String oldPassword, String newPassword);
    /** 获取管理员统计信息 */
    Result getAdminStatistics();
    /** 检查管理员权限 */
    Result checkPermission(String adminId, String permission);
    /** 更新管理员权限 */
    Result updatePermissions(String adminId, List<String> permissions);

    // ===== v1 管理端最小 CRUD（用户 / 商品 / 订单 / 会话/消息）=====
    // 用户
    Result listUsers(Integer page, Integer size, String keyword, String accountStatus, String sortProp, String sortOrder);
    Result getUserById(String userId);
    Result updateUserStatus(String userId, String status);
    Result updateUserBasic(String userId, String nickname, String phone, String email);
    Result deleteUser(String userId);
    /** 完整编辑用户与档案 */
    Result updateUserFull(String userId, java.util.Map<String, Object> payload);

    // 商品
    Result listCommodities(Integer page, Integer size, String keyword, String category, String conditionLevel, String status, String sellerVisibility, String buyerVisibility, String sortProp, String sortOrder);
    Result getCommodityById(String commodityId);
    Result updateCommodityStatus(String commodityId, String status);
    Result deleteCommodity(String commodityId);
    /** 完整编辑商品 */
    Result updateCommodityFull(String commodityId, java.util.Map<String, Object> payload);

    // 订单
    Result listOrders(Integer page, Integer size, String keyword, String status, String sellerVisibility, String buyerVisibility, String sortProp, String sortOrder);
    Result getOrderById(String orderId);
    Result updateOrderFields(String orderId, String status, String trackingNumber, String remark);
    /** 完整更新订单（包括状态和可见性） */
    Result updateOrderFull(String orderId, java.util.Map<String, Object> payload);
    Result deleteOrder(String orderId);

    // 会话/消息
    Result listConversations(Integer page, Integer size, String keyword);
    Result getConversationById(String conversationId);
    Result updateConversationFull(String conversationId, java.util.Map<String, Object> payload);
    Result deleteConversation(String conversationId);
    Result listMessages(String conversationId, Integer page, Integer size);
    Result getMessageById(String messageId);
    Result updateMessageFull(String messageId, java.util.Map<String, Object> payload);
    Result deleteMessage(String messageId);
}

