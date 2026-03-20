package com.njumarket.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公开用户信息DTO（不包含敏感信息）
 * 用于展示给其他用户查看
 */
@Schema(description = "公开用户信息（不含敏感信息）")
@Data
public class PublicUserDTO {

    @Schema(description = "用户ID", example = "USER_123456")
    private String userId;

    @Schema(description = "账户状态", example = "ACTIVE")
    private String accountStatus;

    @Schema(description = "注册时间", example = "2024-01-01T00:00:00")
    private LocalDateTime registerTime;

    @Schema(description = "昵称", example = "小明")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    // 注意：不包含以下敏感信息
    // - primaryPhone (手机号)
    // - password (密码)
    // - email (邮箱)
    // - 详细地址
}

