package com.njumarket.auth.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 修改手机号数据传输对象
 */
@Schema(description = "修改手机号")
@Data
public class UpdatePhoneDTO {

    @NotBlank(message = "新手机号不能为空")
    @Schema(description = "新手机号", example = "13800138000", required = true)
    private String newPhone;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码", example = "123456", required = true)
    private String code;
}

