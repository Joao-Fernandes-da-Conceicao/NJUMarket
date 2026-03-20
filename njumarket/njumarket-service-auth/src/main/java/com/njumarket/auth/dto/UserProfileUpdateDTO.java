package com.njumarket.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户档案更新数据传输对象
 */
@Schema(description = "用户档案更新")
@Data
public class UserProfileUpdateDTO {

    @Schema(description = "昵称", example = "新昵称")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/new-avatar.jpg")
    private String avatar;
}

