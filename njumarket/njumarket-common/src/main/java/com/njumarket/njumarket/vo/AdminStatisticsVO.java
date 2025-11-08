package com.njumarket.njumarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 管理员统计信息VO
 */
@Schema(description = "管理员统计信息")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatisticsVO {
    @Schema(description = "总管理员数", example = "100")
    private Long totalAdmins;

    @Schema(description = "活跃管理员数", example = "80")
    private Long activeAdmins;

    @Schema(description = "各级别管理员数量统计")
    private Map<String, Long> levelStats;

    @Schema(description = "系统管理员数量", example = "10")
    private Long systemAdmins;

    @Schema(description = "普通管理员数量", example = "90")
    private Long administratorCount;
}

