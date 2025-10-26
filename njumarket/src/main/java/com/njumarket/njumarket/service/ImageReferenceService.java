package com.njumarket.njumarket.service;

import com.njumarket.njumarket.entity.ImageReference;

import java.util.List;

public interface ImageReferenceService {
    
    /**
     * 添加图片引用
     * @param imagePath 图片路径
     * @param imageType 图片类型
     * @param uploadUserId 上传者ID
     * @return 图片引用对象
     */
    ImageReference addReference(String imagePath, String imageType, String uploadUserId);
    
    /**
     * 增加图片引用计数
     * @param imagePath 图片路径
     */
    void incrementReference(String imagePath);
    
    /**
     * 减少图片引用计数
     * @param imagePath 图片路径
     * @return 是否应该删除图片
     */
    boolean decrementReference(String imagePath);
    
    /**
     * 批量增加引用
     * @param imagePaths 图片路径列表
     * @param imageType 图片类型
     * @param uploadUserId 上传者ID
     */
    void addReferences(List<String> imagePaths, String imageType, String uploadUserId);
    
    /**
     * 批量减少引用
     * @param imagePaths 图片路径列表
     * @return 需要删除的图片路径列表
     */
    List<String> decrementReferences(List<String> imagePaths);
    
    /**
     * 删除物理文件
     * @param imagePath 图片路径
     */
    void deletePhysicalFile(String imagePath);
    
    /**
     * 清理零引用的图片
     * @return 清理的图片数量
     */
    int cleanupUnreferencedImages();
    
    /**
     * 获取图片引用信息
     * @param imagePath 图片路径
     * @return 图片引用对象
     */
    ImageReference getImageReference(String imagePath);
}

