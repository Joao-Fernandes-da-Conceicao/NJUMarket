package com.njumarket.message.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类（Message Service专用，用于SecurityContext）
 * 只包含基本信息，不持久化到数据库
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String userId;
    private String primaryPhone;
    private String username;
    private String password;
    private LocalDateTime registerTime;
    private String accountStatus; // ACTIVE, SUSPENDED, BANNED
    
    /**
     * 检查用户是否可以登录
     */
    public Boolean canLogin() {
        return "ACTIVE".equals(this.accountStatus);
    }
    
    /**
     * 重写toString方法，避免访问懒加载的集合字段
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

