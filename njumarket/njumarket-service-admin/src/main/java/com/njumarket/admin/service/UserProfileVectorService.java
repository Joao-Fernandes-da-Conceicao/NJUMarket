package com.njumarket.admin.service;

import com.njumarket.njumarket.dto.Result;

import java.util.List;

/**
 * 用户画像向量管理服务接口（管理端）
 * 类似ES索引管理，提供画像向量的CRUD和批量操作
 */
public interface UserProfileVectorService {
    
    /**
     * 重建所有用户画像（类似ES的reindex）
     * 为所有用户重新生成画像向量
     */
    Result rebuildAllProfiles();
    
    /**
     * 为指定用户生成/更新画像向量（类似ES的sync）
     */
    Result generateProfile(String userId);
    
    /**
     * 批量生成/更新用户画像向量
     */
    Result batchGenerateProfiles(List<String> userIds);
    
    /**
     * 删除用户画像向量
     */
    Result deleteProfile(String userId);
    
    /**
     * 批量删除用户画像向量
     */
    Result batchDeleteProfiles(List<String> userIds);
    
    /**
     * 获取用户画像向量详情
     */
    Result getProfileDetail(String userId);
    
    /**
     * 获取画像向量统计信息（类似ES的stats）
     */
    Result getProfileStatistics();
    
    /**
     * 获取画像向量列表（分页）
     */
    Result getProfileList(Integer page, Integer size, String keyword, String sortProp, String sortOrder);
}

