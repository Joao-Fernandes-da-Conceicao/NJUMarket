package com.njumarket.image.repository;

import com.njumarket.njumarket.entity.ImageReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImageReferenceRepository extends JpaRepository<ImageReference, Long> {
    
    // 根据图片路径查找
    Optional<ImageReference> findByImagePath(String imagePath);
    
    // 查找所有可删除的图片（引用计数为0）
    @Query("SELECT i FROM ImageReference i WHERE i.referenceCount <= 0 AND i.isDeleted = false")
    List<ImageReference> findDeletableImages();
    
    // 查找所有已标记删除的图片
    List<ImageReference> findByIsDeletedTrue();
    
    // 查找特定类型的图片
    List<ImageReference> findByImageType(String imageType);
    
    // 查找特定用户上传的图片
    List<ImageReference> findByUploadUserId(String uploadUserId);
    
    // 查找长期未使用的图片（最后引用时间超过指定天数）
    @Query("SELECT i FROM ImageReference i WHERE i.lastReferenceTime < :cutoffTime AND i.referenceCount <= 0")
    List<ImageReference> findUnusedImagesBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // 批量更新引用计数
    @Modifying
    @Query("UPDATE ImageReference i SET i.referenceCount = i.referenceCount + :delta WHERE i.imagePath = :imagePath")
    int updateReferenceCount(@Param("imagePath") String imagePath, @Param("delta") int delta);
    
    // 标记为删除
    @Modifying
    @Query("UPDATE ImageReference i SET i.isDeleted = true, i.deletedTime = :deletedTime WHERE i.imagePath = :imagePath")
    int markAsDeleted(@Param("imagePath") String imagePath, @Param("deletedTime") LocalDateTime deletedTime);
    
    // 统计图片总数
    @Query("SELECT COUNT(i) FROM ImageReference i WHERE i.isDeleted = false")
    long countActiveImages();
    
    // 统计总文件大小
    @Query("SELECT SUM(i.fileSize) FROM ImageReference i WHERE i.isDeleted = false")
    Long getTotalFileSize();
}

