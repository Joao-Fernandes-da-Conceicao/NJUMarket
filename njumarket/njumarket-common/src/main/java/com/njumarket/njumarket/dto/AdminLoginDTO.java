package com.njumarket.njumarket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 管理员登录表单DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员登录表单")
public class AdminLoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "管理员用户名", required = true, example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", required = true, example = "admin123")
    private String password;
}

