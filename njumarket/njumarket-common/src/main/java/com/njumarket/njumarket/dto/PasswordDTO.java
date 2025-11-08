package com.njumarket.njumarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 密码相关数据传输对象
 */
@Schema(description = "密码管理")
@Data
public class PasswordDTO {

    @Schema(description = "原密码", example = "oldpassword123")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "新密码长度不能少于6位")
    @Schema(description = "新密码", example = "newpassword123", required = true)
    private String newPassword;

    @Schema(description = "确认新密码", example = "newpassword123")
    private String confirmPassword;

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码", example = "123456")
    private String code;
}

