package com.njumarket.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 管理员实体类（admin-service 副本）
 * 用于管理端直接访问数据库
 */
@Entity
@Table(name = "admins")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    @Column(name = "admin_id", length = 50)
    private String adminId;

    @Column(name = "username", length = 50, unique = true, nullable = false)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "position", length = 50)
    private String position;

    @Column(name = "admin_level", length = 20, nullable = false)
    private String adminLevel = "administrator"; // system, administrator

    @Column(name = "permissions", length = 1000)
    private String permissions; // JSON格式存储权限列表

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "account_status", length = 20, nullable = false)
    private String accountStatus = "ACTIVE"; // ACTIVE, SUSPENDED, BANNED

    @Column(name = "login_count", nullable = false)
    private Integer loginCount = 0;

    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 检查管理员是否可以登录
     * @return 是否可以登录
     */
    public Boolean canLogin() {
        return "ACTIVE".equals(this.accountStatus);
    }

    /**
     * 检查管理员是否有指定权限
     * @param permission 权限名称
     * @return 是否有权限
     */
    public Boolean hasPermission(String permission) {
        if (permissions == null || permissions.trim().isEmpty()) {
            return false;
        }
        return permissions.contains(permission);
    }

    /**
     * 检查是否为系统管理员
     * @return 是否为系统管理员
     */
    public Boolean isSystemAdmin() {
        return "system".equals(this.adminLevel);
    }

    /**
     * 检查是否为普通管理员
     * @return 是否为普通管理员
     */
    public Boolean isAdministrator() {
        return "administrator".equals(this.adminLevel);
    }

    /**
     * 更新登录信息
     * @param loginIp 登录IP
     */
    public void updateLoginInfo(String loginIp) {
        this.lastLoginTime = LocalDateTime.now();
        this.lastLoginIp = loginIp;
        this.loginCount = (this.loginCount == null ? 0 : this.loginCount) + 1;
    }

    /**
     * 重写toString方法，避免访问懒加载的字段
     * 防止在Spring Security调用getName()时触发LazyInitializationException
     */
    @Override
    public String toString() {
        return "Admin{" +
                "adminId='" + adminId + '\'' +
                ", username='" + username + '\'' +
                ", realName='" + realName + '\'' +
                ", adminLevel='" + adminLevel + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}

