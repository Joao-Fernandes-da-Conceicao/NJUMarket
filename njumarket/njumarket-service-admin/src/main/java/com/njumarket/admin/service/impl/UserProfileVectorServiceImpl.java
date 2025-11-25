package com.njumarket.admin.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.admin.service.UserProfileVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户画像向量管理服务实现类（管理端）
 * 类似ES索引管理，通过调用Auth服务的内部接口实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileVectorServiceImpl implements UserProfileVectorService {
    
    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;
    
    @Value("${njumarket.service.auth.url:http://njumarket-service-auth:8091}")
    private String authServiceUrl;
    
    private static final String VECTOR_TABLE = "nju_market.user_profile_vectors";
    
    @Override
    public Result rebuildAllProfiles() {
        try {
            log.info("管理员请求重建所有用户画像向量");
            
            // 获取所有用户ID
            String sql = "SELECT user_id FROM nju_market.users";
            List<String> userIds = jdbcTemplate.queryForList(sql, String.class);
            
            if (userIds.isEmpty()) {
                return Result.ok("没有用户需要重建画像", Map.of("count", 0));
            }
            
            // 异步为每个用户生成画像（通过调用Auth服务的内部接口）
            int successCount = 0;
            int failCount = 0;
            
            for (String userId : userIds) {
                try {
                    String url = authServiceUrl + "/api/internal/user/" + userId + "/generate-profile-vector";
                    HttpHeaders headers = new HttpHeaders();
                    HttpEntity<String> entity = new HttpEntity<>(headers);
                    
                    ResponseEntity<Result> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        Result.class
                    );
                    
                    if (response.getStatusCode().is2xxSuccessful() && 
                        response.getBody() != null && 
                        response.getBody().getSuccess() != null &&
                        response.getBody().getSuccess()) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    log.warn("为用户生成画像失败: userId={}, error={}", userId, e.getMessage());
                    failCount++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", userIds.size());
            result.put("success", successCount);
            result.put("failed", failCount);
            result.put("message", "重建任务已提交，共 " + userIds.size() + " 个用户，成功 " + successCount + " 个，失败 " + failCount + " 个");
            
            return Result.ok("重建任务已提交", result);
            
        } catch (Exception e) {
            log.error("重建所有用户画像向量失败: error={}", e.getMessage(), e);
            return Result.fail("重建失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result generateProfile(String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Result.fail("用户ID不能为空");
            }
            
            log.info("管理员请求生成用户画像向量: userId={}", userId);
            String url = authServiceUrl + "/api/internal/user/" + userId + "/generate-profile-vector";
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Result> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Result.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Result result = response.getBody();
                log.info("用户画像向量生成成功: userId={}", userId);
                return result;
            } else {
                log.error("用户画像向量生成失败: userId={}, 响应状态码={}", userId, response.getStatusCode());
                return Result.fail("用户画像向量生成失败");
            }
        } catch (Exception e) {
            log.error("用户画像向量生成异常: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("用户画像向量生成失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result batchGenerateProfiles(List<String> userIds) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                return Result.fail("用户ID列表不能为空");
            }
            
            log.info("管理员请求批量生成用户画像向量: count={}", userIds.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (String userId : userIds) {
                try {
                    String url = authServiceUrl + "/api/internal/user/" + userId + "/generate-profile-vector";
                    HttpHeaders headers = new HttpHeaders();
                    HttpEntity<String> entity = new HttpEntity<>(headers);
                    
                    ResponseEntity<Result> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        Result.class
                    );
                    
                    if (response.getStatusCode().is2xxSuccessful() && 
                        response.getBody() != null && 
                        response.getBody().getSuccess() != null &&
                        response.getBody().getSuccess()) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    log.warn("为用户生成画像失败: userId={}, error={}", userId, e.getMessage());
                    failCount++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", userIds.size());
            result.put("success", successCount);
            result.put("failed", failCount);
            
            return Result.ok("批量生成任务已提交，成功 " + successCount + " 个，失败 " + failCount + " 个", result);
            
        } catch (Exception e) {
            log.error("批量生成用户画像向量失败: error={}", e.getMessage(), e);
            return Result.fail("批量生成失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result deleteProfile(String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Result.fail("用户ID不能为空");
            }
            
            log.info("管理员请求删除用户画像向量: userId={}", userId);
            String url = authServiceUrl + "/api/internal/user/" + userId + "/profile-vector";
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Result> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                Result.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Result result = response.getBody();
                log.info("用户画像向量删除成功: userId={}", userId);
                return result;
            } else {
                log.error("用户画像向量删除失败: userId={}, 响应状态码={}", userId, response.getStatusCode());
                return Result.fail("用户画像向量删除失败");
            }
        } catch (Exception e) {
            log.error("用户画像向量删除异常: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("用户画像向量删除失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result batchDeleteProfiles(List<String> userIds) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                return Result.fail("用户ID列表不能为空");
            }
            
            log.info("管理员请求批量删除用户画像向量: count={}", userIds.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (String userId : userIds) {
                try {
                    String url = authServiceUrl + "/api/internal/user/" + userId + "/profile-vector";
                    HttpHeaders headers = new HttpHeaders();
                    HttpEntity<String> entity = new HttpEntity<>(headers);
                    
                    ResponseEntity<Result> response = restTemplate.exchange(
                        url,
                        HttpMethod.DELETE,
                        entity,
                        Result.class
                    );
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    log.warn("删除用户画像失败: userId={}, error={}", userId, e.getMessage());
                    failCount++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", userIds.size());
            result.put("success", successCount);
            result.put("failed", failCount);
            
            return Result.ok("批量删除完成，成功 " + successCount + " 个，失败 " + failCount + " 个", result);
            
        } catch (Exception e) {
            log.error("批量删除用户画像向量失败: error={}", e.getMessage(), e);
            return Result.fail("批量删除失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result getProfileDetail(String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Result.fail("用户ID不能为空");
            }
            
            // 查询画像向量信息
            String sql = "SELECT user_id, content, metadata, created_at, updated_at FROM " + VECTOR_TABLE + " WHERE user_id = ?";
            
            try {
                Map<String, Object> profile = jdbcTemplate.queryForMap(sql, userId);
                
                // 不返回向量数据（太大），只返回元数据
                Map<String, Object> result = new HashMap<>();
                result.put("userId", profile.get("user_id"));
                result.put("content", profile.get("content"));
                result.put("metadata", profile.get("metadata"));
                result.put("createdAt", profile.get("created_at"));
                result.put("updatedAt", profile.get("updated_at"));
                result.put("hasVector", true);
                
                return Result.ok("获取画像详情成功", result);
            } catch (Exception e) {
                // 画像不存在
                return Result.ok("用户画像不存在", Map.of("userId", userId, "hasVector", false));
            }
            
        } catch (Exception e) {
            log.error("获取画像详情失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("获取画像详情失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result getProfileStatistics() {
        try {
            // 统计画像向量数量
            String countSql = "SELECT COUNT(*) FROM " + VECTOR_TABLE;
            Long totalCount = jdbcTemplate.queryForObject(countSql, Long.class);
            
            // 统计最近更新的数量（24小时内）
            String recentSql = "SELECT COUNT(*) FROM " + VECTOR_TABLE + " WHERE updated_at >= NOW() - INTERVAL '24 hours'";
            Long recentCount = jdbcTemplate.queryForObject(recentSql, Long.class);
            
            // 统计总用户数
            String userCountSql = "SELECT COUNT(*) FROM nju_market.users";
            Long userCount = jdbcTemplate.queryForObject(userCountSql, Long.class);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalProfiles", totalCount != null ? totalCount : 0);
            statistics.put("recentUpdated", recentCount != null ? recentCount : 0);
            statistics.put("totalUsers", userCount != null ? userCount : 0);
            statistics.put("coverage", userCount != null && userCount > 0 
                ? String.format("%.2f%%", (totalCount != null ? totalCount : 0) * 100.0 / userCount) 
                : "0%");
            
            return Result.ok("获取统计信息成功", statistics);
            
        } catch (Exception e) {
            log.error("获取画像统计信息失败: error={}", e.getMessage(), e);
            return Result.fail("获取统计信息失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result getProfileList(Integer page, Integer size, String keyword, String sortProp, String sortOrder) {
        try {
            // 参数验证
            int pageNum = (page == null || page < 1) ? 1 : page;
            int pageSize = (size == null || size < 1) ? 10 : (size > 100 ? 100 : size);
            int offset = (pageNum - 1) * pageSize;
            
            // 排序
            String orderBy = "updated_at DESC";
            if (sortProp != null && !sortProp.trim().isEmpty()) {
                String direction = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
                orderBy = sortProp + " " + direction;
            }
            
            // 查询
            String countSql = "SELECT COUNT(*) FROM " + VECTOR_TABLE;
            String listSql = "SELECT user_id, content, metadata, created_at, updated_at FROM " + VECTOR_TABLE 
                + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
            
            Long total = jdbcTemplate.queryForObject(countSql, Long.class);
            List<Map<String, Object>> profiles = jdbcTemplate.queryForList(listSql, pageSize, offset);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("list", profiles);
            result.put("total", total != null ? total : 0);
            result.put("page", pageNum);
            result.put("size", pageSize);
            result.put("pages", total != null ? (int) Math.ceil(total.doubleValue() / pageSize) : 0);
            
            return Result.ok("获取画像列表成功", result);
            
        } catch (Exception e) {
            log.error("获取画像列表失败: error={}", e.getMessage(), e);
            return Result.fail("获取画像列表失败: " + e.getMessage());
        }
    }
}

