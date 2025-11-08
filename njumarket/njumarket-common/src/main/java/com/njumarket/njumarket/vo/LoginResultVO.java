package com.njumarket.njumarket.vo;

import com.njumarket.njumarket.dto.UserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 登录结果VO
 */
@Schema(description = "登录结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultVO {
    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "令牌过期时间（秒）", example = "86400")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserDTO userInfo;

    @Schema(description = "订单提醒状态")
    private Map<String, Boolean> orderReminderStatus;
}

