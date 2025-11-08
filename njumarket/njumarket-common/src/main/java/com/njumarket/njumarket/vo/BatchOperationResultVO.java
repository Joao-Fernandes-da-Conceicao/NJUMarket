package com.njumarket.njumarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量操作结果VO
 */
@Schema(description = "批量操作结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResultVO {
    @Schema(description = "成功数量", example = "8")
    private Integer successCount;

    @Schema(description = "失败数量", example = "2")
    private Integer failCount;

    @Schema(description = "总数量", example = "10")
    private Integer total;
}

