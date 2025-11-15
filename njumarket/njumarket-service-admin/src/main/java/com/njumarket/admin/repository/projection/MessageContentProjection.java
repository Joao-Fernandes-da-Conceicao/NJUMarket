package com.njumarket.admin.repository.projection;

import java.time.LocalDateTime;

/**
 * Message内容投影接口
 * 用于只查询content和createdAt字段，避免回表
 */
public interface MessageContentProjection {
    String getContent();
    LocalDateTime getCreatedAt();
}

