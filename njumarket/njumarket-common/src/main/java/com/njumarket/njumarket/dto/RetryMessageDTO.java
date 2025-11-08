package com.njumarket.njumarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 重试消息DTO
 * 用于存储在Redis中的重试队列
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryMessageDTO {
    /**
     * 接收方用户ID
     */
    private String receiverId;

    /**
     * 消息内容（JSON字符串，包含消息类型和数据）
     */
    private String messageData;

    /**
     * 消息类型：MESSAGE_NEW 或 UNREAD_COUNT_UPDATE
     */
    private String messageType;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 最大重试次数（默认3次）
     */
    private int maxRetries = 3;

    /**
     * 下次重试时间（用于指数退避）
     */
    private LocalDateTime nextRetryTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 计算下次重试时间（指数退避策略）
     * 重试间隔：5秒, 10秒, 20秒（最多3次）
     */
    public void calculateNextRetryTime() {
        // 指数退避：5s, 10s, 20s
        int delaySeconds = 5 * (1 << retryCount); // 5 * 2^retryCount
        this.nextRetryTime = LocalDateTime.now().plusSeconds(delaySeconds);
    }

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount++;
        calculateNextRetryTime();
    }

    /**
     * 检查是否还有重试次数（不检查时间）
     */
    public boolean hasRetryAttempts() {
        return retryCount < maxRetries;
    }

    /**
     * 检查是否到了重试时间
     * 允许1秒的容差，避免因为定时任务执行时间微小差异导致消息无法重试
     */
    public boolean isRetryTimeReached() {
        if (nextRetryTime == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        // 允许1秒的容差：如果当前时间 >= (nextRetryTime - 1秒)，就认为可以重试
        // 这样可以避免因为定时任务执行时间微小差异（几毫秒）导致消息无法处理
        LocalDateTime earliestRetryTime = nextRetryTime.minusSeconds(1);
        return now.isAfter(earliestRetryTime) || now.isEqual(earliestRetryTime);
    }

    /**
     * 检查是否可以重试（同时检查次数和时间）
     * 用于定时任务查询时判断
     */
    public boolean canRetry() {
        return hasRetryAttempts() && isRetryTimeReached();
    }
}

