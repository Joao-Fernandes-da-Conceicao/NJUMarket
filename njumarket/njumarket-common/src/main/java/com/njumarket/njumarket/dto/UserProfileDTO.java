package com.njumarket.njumarket.dto;

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

    @Schema(description = "信用分", example = "100")
    private Integer creditScore;

    @Schema(description = "买家评分", example = "4.8")
    private Double buyerRating;

    @Schema(description = "卖家评分", example = "4.9")
    private Double sellerRating;

    @Schema(description = "总销售数", example = "15")
    private Integer totalSales;

    @Schema(description = "总购买数", example = "23")
    private Integer totalPurchases;

    @Schema(description = "VIP等级", example = "GOLD", allowableValues = {"NORMAL", "BRONZE", "SILVER", "GOLD", "PLATINUM"})
    private String vipLevel;

    @Schema(description = "用户基本信息")
    private UserDTO userInfo;
}

