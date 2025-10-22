package com.njumarket.njumarket.dto;

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
    
    @Schema(description = "个人简介", example = "这是我的个人简介")
    private String bio;
    
    @Schema(description = "联系方式", example = "微信: wechat123")
    private String contact;
    
    @Schema(description = "所在地区", example = "南京市")
    private String location;
}
