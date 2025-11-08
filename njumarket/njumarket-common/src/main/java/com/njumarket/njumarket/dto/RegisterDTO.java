package com.njumarket.njumarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户注册数据传输对象
 */
@Schema(description = "用户注册")
@Data
public class RegisterDTO {

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号", example = "13800138000", required = true)
    private String phone;

    @Schema(description = "用户名（可选）", example = "user123")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    @Schema(description = "密码", example = "password123", required = true)
    private String password;

    @Schema(description = "确认密码", example = "password123", required = true)
    private String confirmPassword;

    @Schema(description = "手机验证码（已废弃，不再需要）", example = "123456", required = false)
    private String code;

    @Schema(description = "昵称（可选）", example = "小明")
    private String nickname;

    @Schema(description = "邀请码（可选）", example = "INV123456")
    private String inviteCode;
}

