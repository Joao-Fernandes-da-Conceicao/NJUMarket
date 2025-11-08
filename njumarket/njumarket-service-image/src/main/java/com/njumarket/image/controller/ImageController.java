package com.njumarket.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Tag(name = "图片访问", description = "头像和商品图片访问接口")
@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    @Value("${app.upload.avatar-path:uploads/avatars}")
    private String avatarUploadPath;
    
    @Value("${app.upload.commodity-path:uploads/commodities}")
    private String commodityUploadPath;

    @Operation(summary = "获取头像图片", description = "根据文件名获取用户头像")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "图片不存在")
    })
    @GetMapping("/avatars/{fileName}")
    public ResponseEntity<Resource> getAvatar(@Parameter(description = "文件名", required = true) @PathVariable String fileName) {
        return getImage(avatarUploadPath, fileName);
    }

    @Operation(summary = "获取商品图片", description = "根据文件名获取商品图片")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "图片不存在")
    })
    @GetMapping("/commodities/{fileName}")
    public ResponseEntity<Resource> getCommodityImage(@Parameter(description = "文件名", required = true) @PathVariable String fileName) {
        return getImage(commodityUploadPath, fileName);
    }

    /**
     * 通用图片获取方法
     */
    private ResponseEntity<Resource> getImage(String uploadPath, String fileName) {
        try {
            Path filePath = Paths.get(uploadPath, fileName);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                log.warn("图片文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // 根据文件扩展名确定Content-Type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // 缓存1小时
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("获取图片失败: path={}, fileName={}, error={}", uploadPath, fileName, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

