package com.njumarket.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类（admin-service 副本）
 * 用于管理端直接访问数据库
 * ⚠️ 注意：移除了跨服务的关联关系（@OneToOne, @OneToMany），只保留基本字段
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @Column(name = "user_id", length = 50)
    private String userId;
    
    @Column(name = "primary_phone", length = 20, unique = true, nullable = false)
    private String primaryPhone;
    
    @Column(name = "username", length = 50, unique = true)
    private String username;
    
    @Column(name = "password", length = 255)
    private String password;
    
    @CreationTimestamp
    @Column(name = "register_time", nullable = false)
    private LocalDateTime registerTime;
    
    @Column(name = "account_status", length = 20, nullable = false)
    private String accountStatus; // ACTIVE, SUSPENDED, BANNED, DELETED
    
    // 临时存储UserProfile（不持久化到数据库，仅用于业务逻辑）
    private transient UserProfile userProfile;
    
    /**
     * 获取用户档案
     * @return 用户档案
     */
    public UserProfile getUserProfile() {
        return userProfile;
    }
    
    /**
     * 设置用户档案
     * @param userProfile 用户档案
     */
    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }
    
    /**
     * 检查用户是否可以发布商品
     * @return 是否可以发布
     */
    public Boolean canPublish() {
        return "ACTIVE".equals(this.accountStatus);
    }
    
    /**
     * 重写toString方法，避免访问懒加载的集合字段
     * 防止在Spring Security调用getName()时触发LazyInitializationException
     */
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", primaryPhone='" + primaryPhone + '\'' +
                ", username='" + username + '\'' +
                ", registerTime=" + registerTime +
                ", accountStatus='" + accountStatus + '\'' +
                '}';
    }
}

