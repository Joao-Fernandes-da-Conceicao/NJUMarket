package com.njumarket.njumarket.utils;

/**
 * Redis常量类
 */
public class RedisConstants {
    // 登录验证码相关
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 5L; // 验证码5分钟过期
    
    // JWT Token相关
    public static final String LOGIN_TOKEN_KEY = "login:token:";
    public static final Long LOGIN_TOKEN_TTL = 24 * 60L; // Token 24小时过期
    
    // 刷新Token相关
    public static final String REFRESH_TOKEN_KEY = "refresh:token:";
    public static final Long REFRESH_TOKEN_TTL = 7 * 24 * 60L; // 刷新Token 7天过期
    
    // 用户信息缓存
    public static final String USER_INFO_KEY = "user:info:";
    public static final Long USER_INFO_TTL = 30L; // 用户信息缓存30分钟
    
    // 商品相关缓存
    public static final String CACHE_COMMODITY_KEY = "cache:commodity:";
    public static final Long CACHE_COMMODITY_TTL = 30L;
    
    // 分布式锁
    public static final String LOCK_COMMODITY_KEY = "lock:commodity:";
    public static final Long LOCK_COMMODITY_TTL = 10L;
    
    public static final String LOCK_USER_KEY = "lock:user:";
    public static final Long LOCK_USER_TTL = 10L;
    
    // 业务相关
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String COMMODITY_LIKED_KEY = "commodity:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String COMMODITY_GEO_KEY = "commodity:geo:";
    public static final String USER_SIGN_KEY = "sign:";
    
    // 限流相关
    public static final String RATE_LIMIT_KEY = "rate:limit:";
    public static final Long RATE_LIMIT_TTL = 1L; // 限流1分钟
    
    // ✅ 增量轮询相关：商品和订单变更记录
    public static final String COMMODITY_CHANGE_KEY = "chat:commodity:change:"; // 商品变更记录
    public static final String ORDER_CHANGE_KEY = "chat:order:change:"; // 订单变更记录
    public static final Long CHANGE_RECORD_TTL = 24 * 60 * 60L; // 变更记录保留24小时（秒）
    
    // ✅ WebSocket消息推送重试队列
    public static final String WEBSOCKET_RETRY_QUEUE_KEY = "websocket:retry:queue"; // WebSocket重试队列
    public static final Long WEBSOCKET_RETRY_TTL = 30 * 60L; // 重试消息保留30分钟（秒）
}
