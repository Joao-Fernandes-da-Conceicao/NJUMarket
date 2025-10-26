package com.njumarket.njumarket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.entity.ImageReference;
import com.njumarket.njumarket.entity.Order;
import com.njumarket.njumarket.entity.UserProfile;
import com.njumarket.njumarket.repository.CommodityRepository;
import com.njumarket.njumarket.repository.ImageReferenceRepository;
import com.njumarket.njumarket.repository.OrderRepository;
import com.njumarket.njumarket.repository.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 图片引用计数初始化测试
 * 用于更新现有数据，为所有图片创建引用计数记录
 */
@Slf4j
@SpringBootTest
public class ImageReferenceInitializationTest {
    
    @Autowired
    private ImageReferenceRepository imageReferenceRepository;
    
    @Autowired
    private CommodityRepository commodityRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String UPLOAD_BASE_PATH = "uploads/";
    
    /**
     * 初始化所有图片的引用计数
     */
    @Test
    @Transactional
    public void initializeImageReferences() {
        log.info("=== 开始初始化图片引用计数 ===");
        
        // 1. 处理商品图片
        int commodityImagesCount = processCommodityImages();
        log.info("处理商品图片完成，共处理 {} 条引用", commodityImagesCount);
        
        // 2. 处理订单快照图片
        int orderImagesCount = processOrderSnapshotImages();
        log.info("处理订单快照图片完成，共处理 {} 条引用", orderImagesCount);
        
        // 3. 处理用户头像
        int avatarCount = processUserAvatars();
        log.info("处理用户头像完成，共处理 {} 条引用", avatarCount);
        
        // 4. 统计结果
        long totalImages = imageReferenceRepository.count();
        long activeImages = imageReferenceRepository.countActiveImages();
        Long totalSize = imageReferenceRepository.getTotalFileSize();
        
        log.info("=== 初始化完成 ===");
        log.info("图片总数: {}", totalImages);
        log.info("活跃图片: {}", activeImages);
        log.info("总文件大小: {} bytes ({} MB)", 
                totalSize != null ? totalSize : 0, 
                totalSize != null ? totalSize / 1024 / 1024 : 0);
    }
    
    /**
     * 处理商品图片
     */
    private int processCommodityImages() {
        List<Commodity> commodities = commodityRepository.findAll();
        int count = 0;
        
        for (Commodity commodity : commodities) {
            try {
                if (commodity.getImages() != null && !commodity.getImages().trim().isEmpty()) {
                    List<String> images = parseImageList(commodity.getImages());
                    
                    for (String imagePath : images) {
                        if (imagePath != null && !imagePath.trim().isEmpty()) {
                            addOrUpdateImageReference(imagePath, "COMMODITY", commodity.getSellerId());
                            count++;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("处理商品图片失败: commodityId={}", commodity.getCommodityId(), e);
            }
        }
        
        return count;
    }
    
    /**
     * 处理订单快照图片
     */
    private int processOrderSnapshotImages() {
        List<Order> orders = orderRepository.findAll();
        int count = 0;
        
        for (Order order : orders) {
            try {
                if (order.getCommoditySnapshotImages() != null && 
                    !order.getCommoditySnapshotImages().trim().isEmpty()) {
                    List<String> images = parseImageList(order.getCommoditySnapshotImages());
                    
                    for (String imagePath : images) {
                        if (imagePath != null && !imagePath.trim().isEmpty()) {
                            addOrUpdateImageReference(imagePath, "COMMODITY", order.getSellerId());
                            count++;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("处理订单快照图片失败: orderId={}", order.getOrderId(), e);
            }
        }
        
        return count;
    }
    
    /**
     * 处理用户头像
     */
    private int processUserAvatars() {
        List<UserProfile> profiles = userProfileRepository.findAll();
        int count = 0;
        
        for (UserProfile profile : profiles) {
            try {
                if (profile.getAvatar() != null && !profile.getAvatar().trim().isEmpty()) {
                    // 跳过默认头像
                    if (!profile.getAvatar().startsWith("default_") && 
                        !profile.getAvatar().startsWith("http")) {
                        addOrUpdateImageReference(profile.getAvatar(), "AVATAR", profile.getUserId());
                        count++;
                    }
                }
            } catch (Exception e) {
                log.error("处理用户头像失败: userId={}", profile.getUserId(), e);
            }
        }
        
        return count;
    }
    
    /**
     * 添加或更新图片引用
     */
    private void addOrUpdateImageReference(String imagePath, String imageType, String uploadUserId) {
        Optional<ImageReference> existingRef = imageReferenceRepository.findByImagePath(imagePath);
        
        if (existingRef.isPresent()) {
            ImageReference imageRef = existingRef.get();
            imageRef.incrementReference();
            imageReferenceRepository.save(imageRef);
            log.debug("增加引用计数: {} -> {}", imagePath, imageRef.getReferenceCount());
        } else {
            ImageReference imageRef = new ImageReference();
            imageRef.setImagePath(imagePath);
            imageRef.setImageType(imageType);
            imageRef.setUploadUserId(uploadUserId);
            imageRef.setReferenceCount(1);
            
            // 尝试获取文件大小
            try {
                Path filePath = Paths.get(UPLOAD_BASE_PATH + imagePath);
                if (Files.exists(filePath)) {
                    imageRef.setFileSize(Files.size(filePath));
                }
            } catch (Exception e) {
                log.warn("无法获取文件大小: {}", imagePath);
            }
            
            imageReferenceRepository.save(imageRef);
            log.debug("创建新引用: {}", imagePath);
        }
    }
    
    /**
     * 解析图片列表（JSON格式）
     */
    private List<String> parseImageList(String imagesJson) {
        try {
            if (imagesJson == null || imagesJson.trim().isEmpty() || "[]".equals(imagesJson)) {
                return new ArrayList<>();
            }
            
            // 尝试解析JSON数组
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("解析图片JSON失败: {}", imagesJson, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 测试：统计图片使用情况
     */
    @Test
    public void statisticsImageUsage() {
        log.info("=== 图片使用情况统计 ===");
        
        // 按类型统计
        List<ImageReference> commodityImages = imageReferenceRepository.findByImageType("COMMODITY");
        List<ImageReference> avatarImages = imageReferenceRepository.findByImageType("AVATAR");
        
        log.info("商品图片: {} 个", commodityImages.size());
        log.info("用户头像: {} 个", avatarImages.size());
        
        // 统计高引用图片
        List<ImageReference> allImages = imageReferenceRepository.findAll();
        allImages.sort((a, b) -> b.getReferenceCount().compareTo(a.getReferenceCount()));
        
        log.info("引用次数最多的前10个图片:");
        for (int i = 0; i < Math.min(10, allImages.size()); i++) {
            ImageReference img = allImages.get(i);
            log.info("  {} - 引用次数: {}", img.getImagePath(), img.getReferenceCount());
        }
        
        // 统计零引用图片
        List<ImageReference> zeroRefImages = imageReferenceRepository.findDeletableImages();
        log.info("零引用图片: {} 个", zeroRefImages.size());
    }
    
    /**
     * 测试：清理零引用图片（仅标记，不删除物理文件）
     */
    @Test
    @Transactional
    public void cleanupUnusedImagesTest() {
        log.info("=== 开始测试清理零引用图片 ===");
        
        List<ImageReference> deletableImages = imageReferenceRepository.findDeletableImages();
        log.info("找到 {} 个零引用图片", deletableImages.size());
        
        for (ImageReference img : deletableImages) {
            log.info("  可删除: {} (引用次数: {})", img.getImagePath(), img.getReferenceCount());
        }
        
        log.info("=== 测试完成（未实际删除） ===");
    }
    
    /**
     * 测试：验证特定图片的引用情况
     */
    @Test
    public void checkSpecificImageReference() {
        log.info("=== 检查特定图片的引用情况 ===");
        
        // 从商品表获取一些图片路径进行检查
        List<Commodity> commodities = commodityRepository.findAll();
        if (!commodities.isEmpty()) {
            Commodity commodity = commodities.get(0);
            if (commodity.getImages() != null) {
                List<String> images = parseImageList(commodity.getImages());
                if (!images.isEmpty()) {
                    String imagePath = images.get(0);
                    Optional<ImageReference> refOpt = imageReferenceRepository.findByImagePath(imagePath);
                    
                    if (refOpt.isPresent()) {
                        ImageReference ref = refOpt.get();
                        log.info("图片: {}", imagePath);
                        log.info("  引用次数: {}", ref.getReferenceCount());
                        log.info("  图片类型: {}", ref.getImageType());
                        log.info("  上传者: {}", ref.getUploadUserId());
                        log.info("  文件大小: {} bytes", ref.getFileSize());
                    } else {
                        log.info("图片未在引用表中: {}", imagePath);
                    }
                }
            }
        }
    }
}
