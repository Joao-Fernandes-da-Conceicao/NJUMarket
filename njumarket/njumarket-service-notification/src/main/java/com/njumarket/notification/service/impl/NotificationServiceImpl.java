package com.njumarket.notification.service.impl;

import com.njumarket.notification.service.NotificationService;
import com.njumarket.notification.service.WebSocketRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一推送服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    private final WebSocketRetryService webSocketRetryService;
    
    @Override
    public void pushOrderChange(String userId, String orderId, String operation, String recipientRole) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("type", "ORDER_CHANGE");
        messageData.put("orderId", orderId);
        messageData.put("operation", operation);
        messageData.put("changeType", operation);
        messageData.put("timestamp", java.time.LocalDateTime.now().toString());

        String orderStatus = inferOrderStatus(operation);
        if (orderStatus != null) {
            messageData.put("orderStatus", orderStatus);
        }

        // 优先使用事件中明确携带的接收方角色，避免对 ORDER_CANCELLED 等双向操作的角色误判；
        // 仅在未携带时才降级为操作类型推断（向后兼容旧的事件生产者）
        String targetRole = (recipientRole != null && !recipientRole.isBlank())
                ? recipientRole
                : inferTargetRole(operation);
        if (targetRole != null) {
            messageData.put("targetRole", targetRole);
        }

        String messageId = "ORDER_CHANGE_" + orderId + "_" + operation + "_" + System.currentTimeMillis();
        messageData.put("messageId", messageId);

        webSocketRetryService.pushWithRetry(userId, messageData, "ORDER_CHANGE", messageId);
    }
    
    /**
     * 根据operation推断订单状态
     */
    private String inferOrderStatus(String operation) {
        if (operation == null) {
            return null;
        }
        
        switch (operation) {
            case "ORDER_CREATED":
                return "CREATED";
            case "ORDER_PAID":
                return "PAID";
            case "ORDER_SHIPPED":
                return "SHIPPED";
            case "ORDER_COMPLETED":
                return "COMPLETED";
            case "ORDER_CANCELLED":
                return "CANCELLED";
            case "REFUND_REQUESTED":
                return "REFUND_REQUESTED";
            case "REFUND_APPROVED":
            case "REFUND_APPROVE":
                return "REFUNDED";
            case "REFUND_REJECTED":
            case "REFUND_REJECT":
                return "PAID"; // 退款被拒绝，订单状态回到已支付
            default:
                return null;
        }
    }
    
    /**
     * 根据operation推断目标角色（通知应该发给谁）
     * 
     * 注意：某些操作（如ORDER_COMPLETED、ORDER_CANCELLED）可能同时涉及卖家和买家
     * 但实际通知时，会根据调用pushOrderChange时传入的userId来确定通知目标
     * 这里返回的是主要的目标角色，用于前端判断角标显示
     */
    private String inferTargetRole(String operation) {
        if (operation == null) {
            return null;
        }
        
        // 卖家应该收到的通知（主要目标）
        if (operation.equals("ORDER_CREATED") ||
            operation.equals("ORDER_PAID") ||
            operation.equals("REFUND_REQUESTED")) {
            return "SELLER";
        }
        
        // 买家应该收到的通知（主要目标）
        if (operation.equals("ORDER_SHIPPED") ||
            operation.equals("REFUND_APPROVED") ||
            operation.equals("REFUND_APPROVE") ||
            operation.equals("REFUND_REJECTED") ||
            operation.equals("REFUND_REJECT") ||
            operation.equals("ORDER_VISIBILITY_RESTORED")) {
            return "BUYER";
        }
        
        // 订单完成：买家确认收货，主要通知卖家
        if (operation.equals("ORDER_COMPLETED")) {
            return "SELLER";
        }
        
        // 订单取消：根据实际情况，可能是买家取消（通知卖家）或卖家取消（通知买家）
        // 这里默认返回SELLER，因为通常买家取消的情况更多
        if (operation.equals("ORDER_CANCELLED")) {
            return "SELLER";
        }
        
        return null;
    }
    
    @Override
    public void pushMessage(String userId, Object messageData) {
        // ✅ 从messageData中提取messageId（如果存在）
        String messageId = null;
        if (messageData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) messageData;
            messageId = (String) dataMap.get("messageId");
        }
        webSocketRetryService.pushWithRetry(userId, messageData, "MESSAGE_NEW", messageId);
    }
    
    @Override
    public void pushUnreadCountUpdate(String userId, Integer unreadCount) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("type", "UNREAD_COUNT_UPDATE");
        messageData.put("unreadCount", unreadCount);
        messageData.put("timestamp", java.time.LocalDateTime.now().toString());
        
        // ✅ 生成messageId用于ACK确认（格式：UNREAD_COUNT_{userId}_{timestamp}）
        String messageId = "UNREAD_COUNT_" + userId + "_" + System.currentTimeMillis();
        messageData.put("messageId", messageId);
        
        webSocketRetryService.pushWithRetry(userId, messageData, "UNREAD_COUNT_UPDATE", messageId);
    }
    
    @Override
    public void pushGenericMessage(String userId, Object messageData, String messageType) {
        // ✅ 从messageData中提取messageId（如果存在）
        String messageId = null;
        if (messageData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) messageData;
            messageId = (String) dataMap.get("messageId");
            // 确保type字段存在
            if (!dataMap.containsKey("type") || dataMap.get("type") == null) {
                dataMap.put("type", messageType);
            }
        }
        webSocketRetryService.pushWithRetry(userId, messageData, messageType, messageId);
    }
}

