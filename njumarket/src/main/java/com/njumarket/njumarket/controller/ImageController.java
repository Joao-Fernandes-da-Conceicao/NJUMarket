package com.njumarket.njumarket.controller;

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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 图片访问控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    @Value("${app.upload.avatar-path:uploads/avatars}")
    private String avatarUploadPath;

    /**
     * 获取头像图片
     */
    @GetMapping("/avatars/{fileName}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(avatarUploadPath, fileName);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                log.warn("头像文件不存在: {}", fileName);
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
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600") // 缓存1小时
                .body(resource);
                
        } catch (IOException e) {
            log.error("获取头像文件失败: fileName={}, error={}", fileName, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取默认头像
     */
    @GetMapping("/avatars/default")
    public ResponseEntity<Resource> getDefaultAvatar() {
        try {
            // 这里可以返回一个默认头像文件
            // 暂时返回404，表示没有默认头像
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("获取默认头像失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
