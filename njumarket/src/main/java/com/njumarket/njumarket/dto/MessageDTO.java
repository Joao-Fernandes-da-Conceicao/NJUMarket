package com.njumarket.njumarket.dto;

import lombok.Data;

/**
 * 消息数据传输对象
 */
@Data
public class MessageDTO {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String content;
    private String messageType;
    private Boolean isRead;
}
