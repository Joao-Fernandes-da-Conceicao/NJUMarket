package com.njumarket.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公开用户档案DTO（不包含敏感信息）
 * 用于其他用户查看
 */
@Schema(description = "公开用户档案信息")
@Data
public class PublicUserProfileDTO {

    @Schema(description = "档案ID", example = "PROFILE_123456")
    private String profileId;

    @Schema(description = "用户ID", example = "USER_123456")
    private String userId;

    @Schema(description = "昵称", example = "小明")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "公开用户基本信息（不含敏感信息）")
    private PublicUserDTO userInfo;

    // 不包含以下敏感信息：
    // - 手机号（primaryPhone）
    // - 密码（password）
    // - 邮箱（email）
    // - 详细地址信息
}

