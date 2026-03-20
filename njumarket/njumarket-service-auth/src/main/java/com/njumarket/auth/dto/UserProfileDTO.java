package com.njumarket.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户档案数据传输对象
 */
@Schema(description = "用户档案信息")
@Data
public class UserProfileDTO {

    @Schema(description = "档案ID", example = "PROFILE_123456")
    private String profileId;

    @Schema(description = "用户ID", example = "USER_123456")
    private String userId;

    @Schema(description = "昵称", example = "小明")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "用户基本信息")
    private UserDTO userInfo;
}

