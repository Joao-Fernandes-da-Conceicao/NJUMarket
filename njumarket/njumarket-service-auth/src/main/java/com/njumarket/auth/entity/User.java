package com.njumarket.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.njumarket.njumarket.model.IUser;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体类
 * 用户既是买家也是卖家，一个手机号只能绑定一个账号
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements IUser {
    
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
    private String accountStatus; // ACTIVE, SUSPENDED, BANNED
    
    // 一对一关系：用户档案
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // 避免Jackson序列化时的循环引用（服务间通信已使用内部DTO，但保留此注解以防直接序列化实体）
    private UserProfile userProfile;
    
    // 一对多关系：用户活动记录
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserActivityRecord> activityRecords;
    
    /**
     * 用户登录
     * @param type 登录类型
     * @return 登录是否成功
     */
    public Boolean login(String type) {
        // 业务逻辑实现
        return true;
    }
    
    /**
     * 绑定第三方账号
     * @param accountType 账号类型
     * @param accountId 账号ID
     * @return 绑定是否成功
     */
    public Boolean bindThirdParty(String accountType, String accountId) {
        // 业务逻辑实现
        return true;
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

