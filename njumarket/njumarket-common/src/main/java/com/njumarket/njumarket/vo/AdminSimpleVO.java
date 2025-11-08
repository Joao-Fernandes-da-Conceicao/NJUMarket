package com.njumarket.njumarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员简单信息VO
 */
@Schema(description = "管理员简单信息")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSimpleVO {
    @Schema(description = "管理员ID")
    private String adminId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "部门")
    private String department;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "管理员级别")
    private String adminLevel;

    @Schema(description = "权限")
    private String permissions;

    @Schema(description = "账户状态")
    private String accountStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "登录次数")
    private Integer loginCount;

    @Schema(description = "备注")
    private String remark;
}

