package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 图片引用计数实体类
 */
@Entity
@Table(name = "image_references")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageReference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;
    
    @Column(name = "image_path", length = 500, nullable = false, unique = true)
    private String imagePath;
    
    @Column(name = "image_type", length = 20, nullable = false)
    private String imageType; // AVATAR, COMMODITY
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "upload_user_id", length = 50)
    private String uploadUserId;
    
    @Column(name = "reference_count", nullable = false)
    private Integer referenceCount = 1;
    
    @Column(name = "upload_time")
    private LocalDateTime uploadTime;
    
    @Column(name = "last_reference_time")
    private LocalDateTime lastReferenceTime;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "deleted_time")
    private LocalDateTime deletedTime;
    
    @PrePersist
    protected void onCreate() {
        if (uploadTime == null) {
            uploadTime = LocalDateTime.now();
        }
        if (lastReferenceTime == null) {
            lastReferenceTime = LocalDateTime.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (referenceCount == null) {
            referenceCount = 1;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastReferenceTime = LocalDateTime.now();
    }
    
    /**
     * 增加引用计数
     */
    public void incrementReference() {
        this.referenceCount++;
        this.lastReferenceTime = LocalDateTime.now();
    }
    
    /**
     * 减少引用计数
     * @return 是否应该删除图片（引用计数为0）
     */
    public boolean decrementReference() {
        if (this.referenceCount > 0) {
            this.referenceCount--;
            this.lastReferenceTime = LocalDateTime.now();
        }
        
        if (this.referenceCount <= 0) {
            this.isDeleted = true;
            this.deletedTime = LocalDateTime.now();
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否可以删除
     */
    public boolean canDelete() {
        return this.referenceCount <= 0 || this.isDeleted;
    }
}

