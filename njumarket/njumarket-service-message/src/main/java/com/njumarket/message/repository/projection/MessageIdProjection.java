package com.njumarket.message.repository.projection;

/**
 * Message ID投影接口
 * 用于只查询messageId字段，避免回表
 */
public interface MessageIdProjection {
    String getMessageId();
}

