package com.njumarket.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "njumarket.admin.cookie")
public class AdminCookieProperties {
    private String sameSite = "Strict";
    private boolean secure = false;
    private String path = "/";
}
