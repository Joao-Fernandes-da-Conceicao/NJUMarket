package com.njumarket.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户登录 Cookie（HttpOnly + SameSite）配置
 */
@Data
@ConfigurationProperties(prefix = "njumarket.auth.cookie")
public class AuthCookieProperties {

    /** SameSite：Strict / Lax / None（None 需配合 Secure） */
    private String sameSite = "Strict";

    /** 仅 HTTPS 发送（生产环境 true） */
    private boolean secure = false;

    private String path = "/";
}
