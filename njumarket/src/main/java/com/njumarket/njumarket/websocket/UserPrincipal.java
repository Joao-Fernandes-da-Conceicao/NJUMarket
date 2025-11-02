package com.njumarket.njumarket.websocket;

import java.security.Principal;

/**
 * WebSocket 用户身份标识
 * 用于 Spring WebSocket 的 convertAndSendToUser() 方法识别用户
 */
public class UserPrincipal implements Principal {
    
    private final String userId;
    
    public UserPrincipal(String userId) {
        this.userId = userId;
    }
    
    @Override
    public String getName() {
        return userId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserPrincipal that = (UserPrincipal) obj;
        return userId != null ? userId.equals(that.userId) : that.userId == null;
    }
    
    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "UserPrincipal{userId='" + userId + "'}";
    }
}

