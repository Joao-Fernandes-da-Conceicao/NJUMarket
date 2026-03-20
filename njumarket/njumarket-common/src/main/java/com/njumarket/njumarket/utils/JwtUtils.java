package com.njumarket.njumarket.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtils {

    // JWT密钥
    private static final String SECRET_KEY = "njumarket_secret_key_for_jwt_token_generation_2024";

    // Token有效期（24小时）
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    // 刷新Token有效期（7天）
    private static final long REFRESH_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;

    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * 生成 Session Token（薄 JWT：只携带 sessionId）
     * 用户信息不再嵌入 JWT，统一存入 Redis Session。
     */
    public String generateSessionToken(String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sid", sessionId);
        claims.put("type", "access");
        return createToken(claims, sessionId, EXPIRATION_TIME);
    }

    /**
     * 生成 Session Refresh Token（薄 JWT：只携带 refreshSessionId）
     */
    public String generateSessionRefreshToken(String refreshSessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sid", refreshSessionId);
        claims.put("type", "refresh");
        return createToken(claims, refreshSessionId, REFRESH_EXPIRATION_TIME);
    }

    /**
     * 从 Session Token 中取出 sessionId
     */
    public String getSessionIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("sid", String.class);
        } catch (Exception e) {
            log.error("获取 sessionId 失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * @deprecated 旧接口：生成含 userId+phone 的 JWT（不含 session 概念）。新代码请用 generateSessionToken。
     */
    @Deprecated
    public String generateToken(String userId, String phone) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("phone", phone);
        claims.put("type", "access");

        return createToken(claims, userId, EXPIRATION_TIME);
    }

    /**
     * @deprecated 旧接口：生成含 userId 的刷新 Token。新代码请用 generateSessionRefreshToken。
     */
    @Deprecated
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");

        return createToken(claims, userId, REFRESH_EXPIRATION_TIME);
    }

    /**
     * 创建Token
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 从Token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String userId = claims.get("userId", String.class);
            // 调试日志：打印解码结果
            if (userId != null) {
                log.debug("JWT解码userId: [{}], 长度={}, 字符数组={}", 
                    userId, userId.length(), java.util.Arrays.toString(userId.toCharArray()));
            }
            return userId;
        } catch (Exception e) {
            log.error("获取用户ID失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从Token中获取手机号
     */
    public String getPhoneFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("phone", String.class);
        } catch (Exception e) {
            log.error("获取手机号失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查Token是否为刷新Token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token中获取Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取Token过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取Token创建时间
     */
    public Date getIssuedAtFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getIssuedAt();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查Token是否已过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("检查Token是否过期失败: {}", e.getMessage());
            return true; // 解析失败视为已过期
        }
    }
}

