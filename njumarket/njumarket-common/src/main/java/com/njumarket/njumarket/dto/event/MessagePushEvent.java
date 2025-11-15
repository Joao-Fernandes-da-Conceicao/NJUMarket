package com.njumarket.njumarket.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息推送事件
 * 用于服务内异步推送消息（解决刷新后重复推送的问题）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessagePushEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 接收者ID
     */
    private String receiverId;
    
    /**
     * 消息ID（用于去重，避免重复推送）
     */
    private String messageId;
    
    /**
     * 消息数据（JSON字符串）
     */
    private String messageData;
    
    /**
     * 消息类型：MESSAGE_NEW, UNREAD_COUNT_UPDATE, CONVERSATION_RESTORED 等
     */
    private String messageType;
}

