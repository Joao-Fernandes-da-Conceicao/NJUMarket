package com.njumarket.njumarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限检查结果VO
 */
@Schema(description = "权限检查结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckVO {
    @Schema(description = "管理员ID", example = "ADMIN_1234567890")
    private String adminId;

    @Schema(description = "权限名称", example = "USER_MANAGE")
    private String permission;

    @Schema(description = "是否有权限", example = "true")
    private Boolean hasPermission;
}

