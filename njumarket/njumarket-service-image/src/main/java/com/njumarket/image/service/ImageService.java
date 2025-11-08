package com.njumarket.image.service;

import com.njumarket.njumarket.dto.ImageUploadDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图片服务接口
 */
public interface ImageService {
    
    /**
     * 上传头像
     */
    ImageUploadDTO uploadAvatar(String userId, MultipartFile file);
    
    /**
     * 上传商品图片（通用，不需要商品ID）
     */
    ImageUploadDTO uploadCommodityImage(String userId, MultipartFile file);
    
    /**
     * 为指定商品上传图片
     */
    ImageUploadDTO uploadCommodityImageForCommodity(String commodityId, MultipartFile file);
    
    /**
     * 批量上传商品图片
     */
    List<ImageUploadDTO> uploadCommodityImages(String commodityId, List<MultipartFile> files);
    
    /**
     * 删除头像
     */
    boolean deleteAvatar(String userId, String fileName);
    
    /**
     * 删除商品图片
     */
    boolean deleteCommodityImage(String commodityId, String fileName);
    
    /**
     * 根据头像URL删除头像文件
     */
    boolean deleteAvatarByUrl(String avatarUrl);
    
    /**
     * 根据商品图片URL删除商品图片文件
     */
    boolean deleteCommodityImageByUrl(String imageUrl);
    
    /**
     * 获取用户头像URL
     */
    String getUserAvatarUrl(String userId);
    
    /**
     * 获取商品图片URL列表
     */
    List<String> getCommodityImageUrls(String commodityId);
    
    /**
     * 验证图片文件
     */
    boolean validateImageFile(MultipartFile file);
    
    /**
     * 生成唯一文件名
     */
    String generateUniqueFileName(String originalFilename, String userId);
    
    /**
     * 获取图片访问URL
     */
    String getImageAccessUrl(String fileName);
}

