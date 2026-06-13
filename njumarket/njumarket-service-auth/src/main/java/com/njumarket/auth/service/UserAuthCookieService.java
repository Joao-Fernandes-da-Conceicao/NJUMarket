package com.njumarket.auth.service;

import com.njumarket.auth.config.AuthCookieProperties;
import com.njumarket.auth.vo.LoginResultVO;
import com.njumarket.auth.vo.TokenResultVO;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.utils.RedisConstants;
import com.njumarket.njumarket.web.AuthCookieHelper;
import com.njumarket.njumarket.web.AuthCookieNames;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 将 Access/Refresh Token 写入 HttpOnly Cookie，并从 JSON 响应体中移除
 */
@Service
@RequiredArgsConstructor
public class UserAuthCookieService {

    private final AuthCookieProperties props;

    public void applyLoginCookiesIfSuccess(HttpServletResponse response, Result result) {
        if (result == null || Boolean.FALSE.equals(result.getSuccess()) || result.getData() == null) {
            return;
        }
        if (!(result.getData() instanceof LoginResultVO vo)) {
            return;
        }
        String access = vo.getAccessToken();
        String refresh = vo.getRefreshToken();
        if (!StringUtils.hasText(access)) {
            return;
        }
        long accessMax = vo.getExpiresIn() != null && vo.getExpiresIn() > 0
                ? vo.getExpiresIn()
                : RedisConstants.SESSION_TTL;
        writePair(response, access, refresh, accessMax);
        vo.setAccessToken(null);
        vo.setRefreshToken(null);
    }

    public void applyRefreshCookiesIfSuccess(HttpServletResponse response, Result result) {
        if (result == null || Boolean.FALSE.equals(result.getSuccess()) || result.getData() == null) {
            return;
        }
        if (!(result.getData() instanceof TokenResultVO vo)) {
            return;
        }
        String access = vo.getAccessToken();
        String refresh = vo.getRefreshToken();
        if (!StringUtils.hasText(access)) {
            return;
        }
        long accessMax = vo.getExpiresIn() != null && vo.getExpiresIn() > 0
                ? vo.getExpiresIn()
                : RedisConstants.SESSION_TTL;
        writePair(response, access, refresh, accessMax);
        vo.setAccessToken(null);
        vo.setRefreshToken(null);
    }

    public void clear(HttpServletResponse response) {
        AuthCookieHelper.clear(response, AuthCookieNames.USER_ACCESS, props.isSecure(), props.getSameSite(), props.getPath());
        AuthCookieHelper.clear(response, AuthCookieNames.USER_REFRESH, props.isSecure(), props.getSameSite(), props.getPath());
    }

    private void writePair(HttpServletResponse response, String accessToken, String refreshToken, long accessMaxAgeSeconds) {
        AuthCookieHelper.add(
                response,
                AuthCookieNames.USER_ACCESS,
                accessToken,
                accessMaxAgeSeconds,
                true,
                props.isSecure(),
                props.getSameSite(),
                props.getPath()
        );
        if (StringUtils.hasText(refreshToken)) {
            AuthCookieHelper.add(
                    response,
                    AuthCookieNames.USER_REFRESH,
                    refreshToken,
                    RedisConstants.SESSION_REFRESH_TTL,
                    true,
                    props.isSecure(),
                    props.getSameSite(),
                    props.getPath()
            );
        }
    }
}
