package com.njumarket.admin.service;

import com.njumarket.admin.config.AdminCookieProperties;
import com.njumarket.admin.vo.AdminLoginResultVO;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.web.AuthCookieHelper;
import com.njumarket.njumarket.web.AuthCookieNames;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminAuthCookieService {

    private final AdminCookieProperties props;

    public void applyLoginCookieIfSuccess(HttpServletResponse response, Result result) {
        if (result == null || Boolean.FALSE.equals(result.getSuccess()) || result.getData() == null) {
            return;
        }
        if (!(result.getData() instanceof AdminLoginResultVO vo)) {
            return;
        }
        String token = vo.getToken();
        if (!StringUtils.hasText(token)) {
            return;
        }
        long maxAge = vo.getExpiresIn() != null && vo.getExpiresIn() > 0 ? vo.getExpiresIn() : 24 * 60 * 60L;
        AuthCookieHelper.add(
                response,
                AuthCookieNames.ADMIN_TOKEN,
                token,
                maxAge,
                true,
                props.isSecure(),
                props.getSameSite(),
                props.getPath()
        );
        vo.setToken(null);
    }

    public void clear(HttpServletResponse response) {
        AuthCookieHelper.clear(response, AuthCookieNames.ADMIN_TOKEN, props.isSecure(), props.getSameSite(), props.getPath());
    }
}
