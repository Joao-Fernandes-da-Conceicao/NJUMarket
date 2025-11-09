package com.njumarket.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员登录结果VO
 */
@Schema(description = "管理员登录结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResultVO {
    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "管理员ID", example = "ADMIN_1234567890")
    private String adminId;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "管理员级别", example = "system")
    private String adminLevel;

    @Schema(description = "令牌过期时间（秒）", example = "86400")
    private Long expiresIn;
}

