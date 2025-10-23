package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.ImageUploadDTO;
import com.njumarket.njumarket.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 图片服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    // 允许的图片类型
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    // 允许的文件扩展名
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "webp"
    );
    
    // 最大文件大小（5MB）
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    // 头像存储路径
    @Value("${app.upload.avatar-path:uploads/avatars}")
    private String avatarUploadPath;
    
    // 商品图片存储路径
    @Value("${app.upload.commodity-path:uploads/commodities}")
    private String commodityUploadPath;
    
    // 图片访问基础URL
    @Value("${app.image.base-url:http://localhost:8080}")
    private String imageBaseUrl;

    @Override
    public ImageUploadDTO uploadAvatar(String userId, MultipartFile file) {
        try {
            // 1. 验证文件
            if (!validateImageFile(file)) {
                throw new IllegalArgumentException("无效的图片文件");
            }
            
            // 2. 生成唯一文件名
            String fileName = generateUniqueFileName(file.getOriginalFilename(), userId);
            
            // 3. 确保上传目录存在
            Path uploadDir = Paths.get(avatarUploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("创建头像上传目录: {}", uploadDir.toAbsolutePath());
            }
            
            // 4. 保存文件
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            
            // 5. 构建响应
            ImageUploadDTO result = new ImageUploadDTO();
            result.setImageUrl(getImageAccessUrl(fileName));
            result.setFileName(fileName);
            result.setFileSize(file.getSize());
            result.setContentType(file.getContentType());
            result.setUploadTime(System.currentTimeMillis());
            
            log.info("头像上传成功: userId={}, fileName={}, size={}", 
                userId, fileName, file.getSize());
            
            return result;
            
        } catch (IOException e) {
            log.error("头像上传失败: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("头像上传失败", e);
        }
    }

    @Override
    public ImageUploadDTO uploadCommodityImage(String commodityId, MultipartFile file) {
        try {
            // 1. 验证文件
            if (!validateImageFile(file)) {
                throw new IllegalArgumentException("无效的图片文件");
            }
            
            // 2. 生成唯一文件名
            String fileName = generateCommodityImageFileName(file.getOriginalFilename(), commodityId);
            
            // 3. 确保上传目录存在
            Path uploadDir = Paths.get(commodityUploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("创建商品图片上传目录: {}", uploadDir.toAbsolutePath());
            }
            
            // 4. 保存文件
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            
            // 5. 构建响应
            ImageUploadDTO result = new ImageUploadDTO();
            result.setImageUrl(getCommodityImageAccessUrl(fileName));
            result.setFileName(fileName);
            result.setFileSize(file.getSize());
            result.setContentType(file.getContentType());
            result.setUploadTime(System.currentTimeMillis());
            
            log.info("商品图片上传成功: commodityId={}, fileName={}, size={}", 
                commodityId, fileName, file.getSize());
            
            return result;
            
        } catch (IOException e) {
            log.error("商品图片上传失败: commodityId={}, error={}", commodityId, e.getMessage());
            throw new RuntimeException("商品图片上传失败", e);
        }
    }

    @Override
    public List<ImageUploadDTO> uploadCommodityImages(String commodityId, List<MultipartFile> files) {
        List<ImageUploadDTO> results = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                ImageUploadDTO result = uploadCommodityImage(commodityId, file);
                results.add(result);
            } catch (Exception e) {
                log.error("批量上传商品图片失败: commodityId={}, fileName={}, error={}", 
                    commodityId, file.getOriginalFilename(), e.getMessage());
                // 继续处理其他文件，不中断整个批量上传过程
            }
        }
        
        log.info("批量上传商品图片完成: commodityId={}, 成功={}, 总数={}", 
            commodityId, results.size(), files.size());
        
        return results;
    }

    @Override
    public boolean deleteAvatar(String userId, String fileName) {
        try {
            Path filePath = Paths.get(avatarUploadPath, fileName);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("头像删除成功: userId={}, fileName={}", userId, fileName);
                return true;
            } else {
                log.warn("头像文件不存在: userId={}, fileName={}", userId, fileName);
                return false;
            }
        } catch (IOException e) {
            log.error("头像删除失败: userId={}, fileName={}, error={}", 
                userId, fileName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteCommodityImage(String commodityId, String fileName) {
        try {
            Path filePath = Paths.get(commodityUploadPath, fileName);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("商品图片删除成功: commodityId={}, fileName={}", commodityId, fileName);
                return true;
            } else {
                log.warn("商品图片文件不存在: commodityId={}, fileName={}", commodityId, fileName);
                return false;
            }
        } catch (IOException e) {
            log.error("商品图片删除失败: commodityId={}, fileName={}, error={}", 
                commodityId, fileName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteAvatarByUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            log.warn("头像URL为空，无法删除");
            return false;
        }
        
        try {
            // 从URL中提取文件名
            String fileName = extractFileNameFromUrl(avatarUrl);
            if (fileName == null) {
                log.warn("无法从URL中提取文件名: {}", avatarUrl);
                return false;
            }
            
            // 删除文件
            Path filePath = Paths.get(avatarUploadPath, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("根据URL删除头像成功: url={}, fileName={}", avatarUrl, fileName);
                return true;
            } else {
                log.warn("根据URL删除头像失败，文件不存在: url={}, fileName={}", avatarUrl, fileName);
                return false;
            }
        } catch (IOException e) {
            log.error("根据URL删除头像失败: url={}, error={}", avatarUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public String getUserAvatarUrl(String userId) {
        // 查找用户的最新头像文件
        try {
            Path avatarDir = Paths.get(avatarUploadPath);
            if (!Files.exists(avatarDir)) {
                return null;
            }
            
            // 查找以userId开头的头像文件
            return Files.list(avatarDir)
                .filter(path -> path.getFileName().toString().contains("_" + userId + "_"))
                .max((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .map(path -> getImageAccessUrl(path.getFileName().toString()))
                .orElse(null);
                
        } catch (IOException e) {
            log.error("获取用户头像URL失败: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean deleteCommodityImageByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            log.warn("商品图片URL为空，无法删除");
            return false;
        }
        
        try {
            // 从URL中提取文件名
            String fileName = extractFileNameFromUrl(imageUrl);
            if (fileName == null) {
                log.warn("无法从URL中提取文件名: {}", imageUrl);
                return false;
            }
            
            // 删除文件
            Path filePath = Paths.get(commodityUploadPath, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("根据URL删除商品图片成功: url={}, fileName={}", imageUrl, fileName);
                return true;
            } else {
                log.warn("根据URL删除商品图片失败，文件不存在: url={}, fileName={}", imageUrl, fileName);
                return false;
            }
        } catch (IOException e) {
            log.error("根据URL删除商品图片失败: url={}, error={}", imageUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> getCommodityImageUrls(String commodityId) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            Path commodityDir = Paths.get(commodityUploadPath);
            if (!Files.exists(commodityDir)) {
                return imageUrls;
            }
            
            // 查找以commodityId开头的图片文件
            Files.list(commodityDir)
                .filter(path -> path.getFileName().toString().contains("_" + commodityId + "_"))
                .sorted((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .forEach(path -> imageUrls.add(getCommodityImageAccessUrl(path.getFileName().toString())));
                
        } catch (IOException e) {
            log.error("获取商品图片URL列表失败: commodityId={}, error={}", commodityId, e.getMessage());
        }
        
        return imageUrls;
    }

    @Override
    public boolean validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("文件为空");
            return false;
        }
        
        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("文件过大: {} bytes", file.getSize());
            return false;
        }
        
        // 检查内容类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("不支持的文件类型: {}", contentType);
            return false;
        }
        
        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            log.warn("文件名为空");
            return false;
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("不支持的文件扩展名: {}", extension);
            return false;
        }
        
        return true;
    }

    @Override
    public String generateUniqueFileName(String originalFilename, String userId) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        return String.format("%s_avatar_%s_%s.%s", timestamp, userId, uuid, extension);
    }
    
    /**
     * 生成商品图片唯一文件名
     */
    public String generateCommodityImageFileName(String originalFilename, String commodityId) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        return String.format("%s_commodity_%s_%s.%s", timestamp, commodityId, uuid, extension);
    }

    @Override
    public String getImageAccessUrl(String fileName) {
        return imageBaseUrl + "/api/images/avatars/" + fileName;
    }
    
    /**
     * 获取商品图片访问URL
     */
    public String getCommodityImageAccessUrl(String fileName) {
        return imageBaseUrl + "/api/images/commodities/" + fileName;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * 从头像URL中提取文件名
     */
    private String extractFileNameFromUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 头像URL格式: http://localhost:8080/api/images/avatars/filename.png
            String avatarPath = "/api/images/avatars/";
            int pathIndex = avatarUrl.indexOf(avatarPath);
            
            if (pathIndex != -1) {
                String fileName = avatarUrl.substring(pathIndex + avatarPath.length());
                // 移除可能的查询参数
                int queryIndex = fileName.indexOf('?');
                if (queryIndex != -1) {
                    fileName = fileName.substring(0, queryIndex);
                }
                return fileName;
            }
            
            // 如果没有找到标准路径，尝试从URL末尾提取文件名
            int lastSlashIndex = avatarUrl.lastIndexOf('/');
            if (lastSlashIndex != -1 && lastSlashIndex < avatarUrl.length() - 1) {
                String fileName = avatarUrl.substring(lastSlashIndex + 1);
                int queryIndex = fileName.indexOf('?');
                if (queryIndex != -1) {
                    fileName = fileName.substring(0, queryIndex);
                }
                return fileName;
            }
            
            return null;
        } catch (Exception e) {
            log.error("从URL提取文件名失败: url={}, error={}", avatarUrl, e.getMessage());
            return null;
        }
    }
}
