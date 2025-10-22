package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户活动记录实体类
 * 记录用户的各种活动行为
 */
@Entity
@Table(name = "user_activity_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityRecord {
    
    @Id
    @Column(name = "record_id", length = 50)
    private String recordId;
    
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;
    
    @Column(name = "activity_type", length = 50, nullable = false)
    private String activityType; // LOGIN, PUBLISH, PURCHASE, BROWSE, SEARCH
    
    @CreationTimestamp
    @Column(name = "activity_time", nullable = false)
    private LocalDateTime activityTime;
    
    @Column(name = "activity_data", columnDefinition = "TEXT")
    private String activityData; // JSON格式存储活动相关数据
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    // 多对一关系：活动记录属于某个用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    /**
     * 记录用户活动
     * @return 记录是否成功
     */
    public Boolean recordActivity() {
        this.activityTime = LocalDateTime.now();
        return true;
    }
}
