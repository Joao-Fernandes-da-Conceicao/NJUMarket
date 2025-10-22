package com.njumarket.njumarket.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.PromotionDTO;

/**
 * 管理员服务接口
 */
public interface AdminService {
    
    // ========== 用户管理 ==========
    /**
     * 获取用户列表
     */
    Result getUserList(Integer page, Integer size, String status);
    
    /**
     * 封禁用户
     */
    Result banUser(String userId, String reason, String banType, String endTime);
    
    /**
     * 解封用户
     */
    Result unbanUser(String userId);
    
    /**
     * 获取用户详细信息
     */
    Result getUserDetail(String userId);
    
    /**
     * 更新用户VIP等级
     */
    Result updateUserVipLevel(String userId, String vipLevel);
    
    /**
     * 获取封禁记录
     */
    Result getBanRecords(Integer page, Integer size);
    
    // ========== 商品管理 ==========
    /**
     * 获取待审核商品列表
     */
    Result getPendingAudits(Integer page, Integer size);
    
    /**
     * 审核商品
     */
    Result auditCommodity(String commodityId, String decision, String reason);
    
    /**
     * 批量审核商品
     */
    Result batchAudit(String[] commodityIds, String decision);
    
    /**
     * 获取商品列表
     */
    Result getCommodityList(Integer page, Integer size, String status);
    
    /**
     * 强制下架商品
     */
    Result removeCommodity(String commodityId, String reason);
    
    /**
     * 获取审核记录
     */
    Result getAuditRecords(Integer page, Integer size);
    
    // ========== 投诉管理 ==========
    /**
     * 获取投诉列表
     */
    Result getComplaints(Integer page, Integer size, String status);
    
    /**
     * 处理投诉
     */
    Result handleComplaint(String complaintId, String decision, String remark);
    
    /**
     * 获取投诉详情
     */
    Result getComplaintDetail(String complaintId);
    
    /**
     * 批量处理投诉
     */
    Result batchHandleComplaints(String[] complaintIds, String decision);
    
    /**
     * 获取投诉统计
     */
    Result getComplaintStatistics();
    
    // ========== 数据统计 ==========
    /**
     * 获取平台概览
     */
    Result getPlatformOverview();
    
    /**
     * 获取用户统计数据
     */
    Result getUserStatistics(String period);
    
    /**
     * 获取商品统计数据
     */
    Result getCommodityStatistics(String period);
    
    /**
     * 获取订单统计数据
     */
    Result getOrderStatistics(String period);
    
    /**
     * 获取交易额统计
     */
    Result getRevenueStatistics(String period, String category);
    
    /**
     * 导出统计报表
     */
    Result exportStatistics(String type, String startDate, String endDate);
    
    /**
     * 获取热门商品排行
     */
    Result getHotCommodities(Integer limit);
    
    /**
     * 获取活跃用户排行
     */
    Result getActiveUsers(Integer limit);
    
    // ========== 促销管理 ==========
    /**
     * 创建促销活动
     */
    Result createPromotion(PromotionDTO promotionDTO);
    
    /**
     * 获取促销活动列表
     */
    Result getPromotions(Integer page, Integer size, String status);
    
    /**
     * 更新促销活动
     */
    Result updatePromotion(String promotionId, PromotionDTO promotionDTO);
    
    /**
     * 激活促销活动
     */
    Result activatePromotion(String promotionId);
    
    /**
     * 停用促销活动
     */
    Result deactivatePromotion(String promotionId);
    
    /**
     * 删除促销活动
     */
    Result deletePromotion(String promotionId);
    
    /**
     * 获取促销活动统计
     */
    Result getPromotionStatistics(String promotionId);
    
    // ========== 内部方法 ==========
    /**
     * 自动审核
     */
    Boolean autoAudit(String commodityId);
}
