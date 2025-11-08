package com.njumarket.njumarket.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录表单数据传输对象
 */
@Schema(description = "登录表单")
@Data
public class LoginFormDTO {
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "用户名或手机号不能为空")
    @Schema(description = "用户名或手机号", example = "user123")
    private String identifier;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "验证码", example = "123456")
    private String code;

    @Schema(description = "登录类型", example = "PASSWORD", allowableValues = {"PHONE", "PASSWORD", "WECHAT", "QQ"})
    private String loginType; // PHONE, PASSWORD, WECHAT, QQ
}

