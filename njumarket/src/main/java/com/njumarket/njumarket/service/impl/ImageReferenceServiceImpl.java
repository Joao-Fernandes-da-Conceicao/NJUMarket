package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.entity.ImageReference;
import com.njumarket.njumarket.repository.ImageReferenceRepository;
import com.njumarket.njumarket.service.ImageReferenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ImageReferenceServiceImpl implements ImageReferenceService {
    
    private final ImageReferenceRepository imageReferenceRepository;
    
    private static final String UPLOAD_BASE_PATH = "uploads/";
    
    @Override
    public ImageReference addReference(String imagePath, String imageType, String uploadUserId) {
        Optional<ImageReference> existingRef = imageReferenceRepository.findByImagePath(imagePath);
        
        if (existingRef.isPresent()) {
            // 图片已存在，增加引用计数
            ImageReference imageRef = existingRef.get();
            imageRef.incrementReference();
            return imageReferenceRepository.save(imageRef);
        } else {
            // 创建新的图片引用记录
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
                log.warn("无法获取文件大小: {}", imagePath, e);
            }
            
            return imageReferenceRepository.save(imageRef);
        }
    }
    
    @Override
    public void incrementReference(String imagePath) {
        Optional<ImageReference> refOpt = imageReferenceRepository.findByImagePath(imagePath);
        if (refOpt.isPresent()) {
            ImageReference imageRef = refOpt.get();
            imageRef.incrementReference();
            imageReferenceRepository.save(imageRef);
        } else {
            log.warn("图片引用不存在，无法增加引用计数: {}", imagePath);
        }
    }
    
    @Override
    public boolean decrementReference(String imagePath) {
        Optional<ImageReference> refOpt = imageReferenceRepository.findByImagePath(imagePath);
        if (refOpt.isPresent()) {
            ImageReference imageRef = refOpt.get();
            boolean shouldDelete = imageRef.decrementReference();
            imageReferenceRepository.save(imageRef);
            
            if (shouldDelete) {
                deletePhysicalFile(imagePath);
                return true;
            }
        } else {
            log.warn("图片引用不存在，无法减少引用计数: {}", imagePath);
        }
        return false;
    }
    
    @Override
    public void addReferences(List<String> imagePaths, String imageType, String uploadUserId) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return;
        }
        
        for (String imagePath : imagePaths) {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                addReference(imagePath, imageType, uploadUserId);
            }
        }
    }
    
    @Override
    public List<String> decrementReferences(List<String> imagePaths) {
        List<String> deletedImages = new ArrayList<>();
        
        if (imagePaths == null || imagePaths.isEmpty()) {
            return deletedImages;
        }
        
        for (String imagePath : imagePaths) {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                if (decrementReference(imagePath)) {
                    deletedImages.add(imagePath);
                }
            }
        }
        
        return deletedImages;
    }
    
    @Override
    public void deletePhysicalFile(String imagePath) {
        try {
            Path filePath = Paths.get(UPLOAD_BASE_PATH + imagePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("删除物理文件成功: {}", imagePath);
            } else {
                log.warn("物理文件不存在: {}", imagePath);
            }
        } catch (Exception e) {
            log.error("删除物理文件失败: {}", imagePath, e);
        }
    }
    
    @Override
    public int cleanupUnreferencedImages() {
        List<ImageReference> deletableImages = imageReferenceRepository.findDeletableImages();
        int count = 0;
        
        for (ImageReference imageRef : deletableImages) {
            imageRef.setIsDeleted(true);
            imageRef.setDeletedTime(LocalDateTime.now());
            imageReferenceRepository.save(imageRef);
            deletePhysicalFile(imageRef.getImagePath());
            count++;
        }
        
        log.info("清理了 {} 个零引用图片", count);
        return count;
    }
    
    @Override
    public ImageReference getImageReference(String imagePath) {
        return imageReferenceRepository.findByImagePath(imagePath).orElse(null);
    }
}

