package com.njumarket.njumarket.web;

/**
 * HttpOnly Cookie 名称（用户端与网关、Auth 服务约定一致）
 */
public final class AuthCookieNames {

    private AuthCookieNames() {}

    /** 用户 Access JWT（与网关校验一致） */
    public static final String USER_ACCESS = "NJ_ACCESS_TOKEN";

    /** 用户 Refresh JWT */
    public static final String USER_REFRESH = "NJ_REFRESH_TOKEN";

    /** 管理端 JWT */
    public static final String ADMIN_TOKEN = "NJ_ADMIN_TOKEN";
}
