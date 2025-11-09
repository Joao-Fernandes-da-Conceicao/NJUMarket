package com.njumarket.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图片上传响应DTO
 */
@Schema(description = "图片上传响应")
@Data
public class ImageUploadDTO {

    @Schema(description = "是否成功", example = "true")
    private boolean success;

    @Schema(description = "消息", example = "上传成功")
    private String message;

    @Schema(description = "图片URL", example = "http://localhost:8080/api/images/avatars/20241022_avatar_123456.jpg")
    private String imageUrl;

    @Schema(description = "图片文件名", example = "20241022_avatar_123456.jpg")
    private String fileName;

    @Schema(description = "图片大小（字节）", example = "102400")
    private Long fileSize;

    @Schema(description = "图片类型", example = "image/jpeg")
    private String contentType;

    @Schema(description = "上传时间戳", example = "1729593024123")
    private Long uploadTime;
}

