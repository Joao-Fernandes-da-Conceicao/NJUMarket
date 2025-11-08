package com.njumarket.njumarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token结果VO
 */
@Schema(description = "Token结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResultVO {
    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "令牌过期时间（秒）", example = "86400")
    private Long expiresIn;
}

