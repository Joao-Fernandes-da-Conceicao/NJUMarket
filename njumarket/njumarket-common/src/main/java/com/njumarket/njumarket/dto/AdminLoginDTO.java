package com.njumarket.njumarket.dto;

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

    @Schema(description = "管理员用户名", required = true, example = "admin")
    private String username;

    @Schema(description = "密码", required = true, example = "admin123")
    private String password;
}

