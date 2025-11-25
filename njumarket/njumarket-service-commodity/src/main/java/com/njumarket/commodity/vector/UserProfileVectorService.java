package com.njumarket.commodity.vector;

/**
 * 用户画像向量化服务接口
 */
public interface UserProfileVectorService {
    
    /**
     * 为用户生成画像向量并存储
     * @param userId 用户ID
     */
    void generateAndStoreUserProfileVector(String userId);
    
    /**
     * 更新用户画像向量
     * @param userId 用户ID
     */
    void updateUserProfileVector(String userId);
    
    /**
     * 删除用户画像向量
     * @param userId 用户ID
     */
    void deleteUserProfileVector(String userId);
    
    /**
     * 获取用户画像向量（用于相似度搜索）
     * @param userId 用户ID
     * @return 用户画像向量，如果不存在返回null
     */
    java.util.List<Double> getUserProfileVector(String userId);
}

