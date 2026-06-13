package com.njumarket.njumarket.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * 写入 HttpOnly、SameSite 约束的 Cookie（不将 token 暴露给 document.cookie）
 */
public final class AuthCookieHelper {

    private AuthCookieHelper() {}

    public static void add(
            HttpServletResponse response,
            String name,
            String value,
            long maxAgeSeconds,
            boolean httpOnly,
            boolean secure,
            String sameSite,
            String path
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .path(path)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** 清除 Cookie（Max-Age=0） */
    public static void clear(
            HttpServletResponse response,
            String name,
            boolean secure,
            String sameSite,
            String path
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(Duration.ZERO)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
