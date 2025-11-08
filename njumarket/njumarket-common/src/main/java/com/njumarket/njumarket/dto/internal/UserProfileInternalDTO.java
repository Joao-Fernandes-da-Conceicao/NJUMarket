package com.njumarket.njumarket.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户档案内部传输对象（用于服务间通信）
 * 不包含关联对象，只包含必要字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileInternalDTO implements Serializable {
    private String profileId;
    private String userId;
    private String nickname;
    private String avatar;
    private String location;
    private String bio;
    private Boolean sellerOrderHasNew;
    private Boolean buyerOrderHasNew;
    private Integer creditScore;
    private Double buyerRating;
    private Double sellerRating;
    // 不包含 User 关联对象
}

