package com.njumarket.njumarket.entity;

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
    private String accountStatus; // ACTIVE, SUSPENDED, BANNED
    
    // 一对多关系：用户拥有多个联系方式
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ContactInfo> contactInfos;
    
    // 一对多关系：用户发布的商品
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Commodity> publishedCommodities;
    
    // 一对多关系：用户作为买家的订单
    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> buyerOrders;
    
    // 一对多关系：用户作为卖家的订单
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> sellerOrders;
    
    // 一对多关系：用户发送的消息
    // 注释掉以避免映射错误，实际通过MessageRepository查询
    // @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Message> sentMessages;
    
    // 一对多关系：用户接收的消息
    // 注释掉以避免映射错误，实际通过MessageRepository查询
    // @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Message> receivedMessages;
    
    // 一对多关系：用户提交的投诉
    @OneToMany(mappedBy = "complainant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Complaint> submittedComplaints;
    
    // 一对多关系：用户被投诉的记录
    @OneToMany(mappedBy = "defendant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Complaint> receivedComplaints;
    
    // 一对一关系：用户档案
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
}
