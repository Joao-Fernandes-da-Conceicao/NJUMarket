package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 封禁记录实体类
 * 记录用户封禁的详细信息
 */
@Entity
@Table(name = "ban_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BanRecord {
    
    @Id
    @Column(name = "ban_id", length = 50)
    private String banId;
    
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "device_id", length = 100)
    private String deviceId;
    
    @Column(name = "real_name_id", length = 50)
    private String realNameId;
    
    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;
    
    @CreationTimestamp
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;
    
    @Column(name = "end_at")
    private LocalDateTime endAt;
    
    @Column(name = "ban_type", length = 20, nullable = false)
    private String banType; // TEMPORARY, PERMANENT, DEVICE, PHONE, REAL_NAME
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /**
     * 创建封禁记录
     * @return 创建是否成功
     */
    public Boolean createBan() {
        this.startAt = LocalDateTime.now();
        this.isActive = true;
        return true;
    }
}
