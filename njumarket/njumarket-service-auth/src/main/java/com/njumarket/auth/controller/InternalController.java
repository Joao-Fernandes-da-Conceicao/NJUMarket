package com.njumarket.auth.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.auth.dto.internal.UserInternalDTOConverter;
import com.njumarket.auth.dto.internal.UserProfileInternalDTOConverter;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.auth.entity.User;
import com.njumarket.auth.entity.UserProfile;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.auth.repository.UserRepository;
import com.njumarket.auth.repository.UserProfileRepository;
import com.njumarket.auth.repository.UserAddressRepository;
import com.njumarket.auth.service.UserProfileService;
import com.njumarket.auth.service.UserAddressService;
import com.njumarket.auth.entity.UserAddress;
import com.njumarket.auth.vector.UserProfileVectorService;
import com.njumarket.njumarket.dto.internal.AddressInternalDTO;
import com.njumarket.njumarket.utils.CacheUtil;
import com.njumarket.njumarket.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内部API控制器
 * 用于微服务间调用，不对外暴露
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserProfileService userProfileService;
    private final UserAddressService userAddressService;
    private final UserInternalDTOConverter userInternalDTOConverter;
    private final UserProfileInternalDTOConverter userProfileInternalDTOConverter;
    private final CacheUtil cacheUtil;
    private final UserProfileVectorService userProfileVectorService;
    
    /**
     * 根据ID查询用户（内部接口）
     * 返回内部 DTO，不包含关联对象
     * ✅ 使用缓存（最终一致性）
     */
    @GetMapping("/user/{userId}")
    public Result getUserById(@PathVariable String userId) {
        // ✅ 使用缓存（最终一致性）
        // ⚠️ 修复：使用独立的缓存key，避免与getUserProfilesByIds的缓存key冲突
        String cacheKey = RedisConstants.CACHE_USER_INFO_KEY + userId;
        UserInternalDTO dto = cacheUtil.getWithFallback(
            cacheKey,
            RedisConstants.CACHE_USER_INFO_TTL * 60, // 转换为秒
            UserInternalDTO.class,
            () -> {
                // 缓存未命中，从数据库加载
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("用户不存在"));
                
                // 调试：打印用户状态信息
                log.info("auth-service查询用户: userId={}, accountStatus=[{}], accountStatus是否为null={}", 
                    user.getUserId(), user.getAccountStatus(), user.getAccountStatus() == null);
                
                UserInternalDTO result = userInternalDTOConverter.toInternalDTO(user);
                
                // 调试：打印DTO状态信息
                log.info("auth-service返回UserInternalDTO: userId={}, accountStatus=[{}], accountStatus是否为null={}", 
                    result.getUserId(), result.getAccountStatus(), result.getAccountStatus() == null);
                
                return result;
            }
        );
        
        // ✅ 后处理：确保accountStatus不为null（处理缓存中可能存在的旧数据）
        if (dto != null && (dto.getAccountStatus() == null || dto.getAccountStatus().trim().isEmpty())) {
            log.warn("检测到缓存中的accountStatus为null，修复为ACTIVE: userId={}", userId);
            dto.setAccountStatus("ACTIVE");
            // 重新写入缓存，修复旧数据
            cacheUtil.set(cacheKey, dto, RedisConstants.CACHE_USER_INFO_TTL * 60);
        }
        
        return Result.ok("查询成功", dto);
    }
    
    /**
     * 批量查询用户（内部接口）
     * 返回内部 DTO 列表，不包含关联对象
     */
    @GetMapping("/user/batch")
    public Result getUsersByIds(@RequestParam List<String> userIds) {
            List<User> users = userRepository.findAllById(userIds);
            List<UserInternalDTO> dtos = userInternalDTOConverter.toUserInternalDTOList(users);
            return Result.ok("批量查询成功", dtos);
    }
    
    /**
     * 批量查询用户档案（内部接口）
     * 返回内部 DTO 列表，不包含关联对象
     * ✅ 使用缓存（最终一致性）- 批量查询时优先从缓存获取，缺失的再从数据库加载
     */
    @GetMapping("/user/profile/batch")
    public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
        List<UserProfileInternalDTO> dtos = new ArrayList<>();
        List<String> missingUserIds = new ArrayList<>();
        
        // 1. 先从缓存获取已有的用户档案
        for (String userId : userIds) {
            String cacheKey = RedisConstants.CACHE_USER_PROFILE_KEY + userId;
            UserProfileInternalDTO cached = cacheUtil.get(cacheKey, UserProfileInternalDTO.class);
            if (cached != null) {
                dtos.add(cached);
            } else {
                missingUserIds.add(userId);
            }
        }
        
        // 2. 批量查询缺失的用户档案
        if (!missingUserIds.isEmpty()) {
            List<UserProfile> profiles = userProfileRepository.findByUserIdIn(missingUserIds);
            List<UserProfileInternalDTO> newDtos = userProfileInternalDTOConverter.toUserProfileInternalDTOList(profiles);
            
            // 3. 将新查询的档案写入缓存
            for (UserProfileInternalDTO dto : newDtos) {
                String cacheKey = RedisConstants.CACHE_USER_PROFILE_KEY + dto.getUserId();
                cacheUtil.set(cacheKey, dto, RedisConstants.CACHE_USER_PROFILE_TTL * 60);
            }
            
            dtos.addAll(newDtos);
        }
        
        return Result.ok("批量查询成功", dtos);
    }
    
    /**
     * 更新用户完整信息（管理端内部接口）
     */
    @PutMapping("/user/{userId}/full")
    public Result updateUserFull(@PathVariable String userId, @RequestBody Map<String, Object> payload) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));
            
            // 更新基本字段
            // ✅ 标记是否修改了用户基本信息（用于缓存删除）
            boolean userInfoChanged = false;
            
            Object username = payload.get("username");
            if (username instanceof String && StringUtils.hasText((String) username)) {
                user.setUsername(((String) username).trim());
                userInfoChanged = true; // username变更需要删除缓存
            }
            Object primaryPhone = payload.get("primaryPhone");
            if (primaryPhone instanceof String && StringUtils.hasText((String) primaryPhone)) {
                user.setPrimaryPhone(((String) primaryPhone).trim());
                userInfoChanged = true; // primaryPhone变更需要删除缓存
            }
            Object accountStatus = payload.get("accountStatus");
            if (accountStatus instanceof String && StringUtils.hasText((String) accountStatus)) {
                String newStatus = ((String) accountStatus).trim();
                java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList("ACTIVE","SUSPENDED","BANNED"));
                if (allowed.contains(newStatus)) {
                    user.setAccountStatus(newStatus);
                    userInfoChanged = true; // accountStatus变更需要删除缓存
                }
            }
            
            // 更新档案字段
            // ✅ 先通过Repository查询，避免JPA延迟加载导致的null判断错误
            UserProfile profile = userProfileRepository.findByUserId(user.getUserId())
                .orElse(null);
            
            if (profile == null) {
                // 如果确实不存在，创建新的档案
                profile = new UserProfile();
                profile.setProfileId("PROFILE_" + System.currentTimeMillis());
                profile.setUserId(user.getUserId());
                // 设置默认值
                profile.setCreditScore(100);
                profile.setBuyerRating(5.0);
                profile.setSellerRating(5.0);
                profile.setTotalSales(0);
                profile.setTotalPurchases(0);
                profile.setVipLevel("NORMAL");
            }
            
            // 更新字段（如果payload中有提供）
            Object nickname = payload.get("nickname");
            if (nickname instanceof String && StringUtils.hasText((String) nickname)) {
                profile.setNickname(((String) nickname).trim());
            }
            Object avatar = payload.get("avatar");
            if (avatar instanceof String && StringUtils.hasText((String) avatar)) {
                profile.setAvatar(((String) avatar).trim());
            }
            
            userRepository.save(user);
            userProfileRepository.save(profile);
            
            // ✅ Cache Aside模式：先更新数据库，再删除缓存
            // 删除用户信息和档案缓存（username、primaryPhone、accountStatus或档案信息变更后需要清除缓存）
            String userInfoCacheKey = RedisConstants.CACHE_USER_INFO_KEY + userId;
            String userProfileCacheKey = RedisConstants.CACHE_USER_PROFILE_KEY + userId;
            cacheUtil.delete(userInfoCacheKey);
            cacheUtil.delete(userProfileCacheKey);
            log.info("已删除用户信息和档案缓存（Cache Aside模式）: userId={}, userInfoChanged={}", userId, userInfoChanged);
            
            return Result.ok("更新成功");
    }
    
    /**
     * 删除用户（管理端内部接口）
     */
    @DeleteMapping("/user/{userId}")
    public Result deleteUser(@PathVariable String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));
            user.setAccountStatus("DELETED");
            userRepository.save(user);
            
            // ✅ 删除用户信息和档案缓存（用户删除后需要清除缓存）
            String userInfoCacheKey = RedisConstants.CACHE_USER_INFO_KEY + userId;
            String userProfileCacheKey = RedisConstants.CACHE_USER_PROFILE_KEY + userId;
            cacheUtil.delete(userInfoCacheKey);
            cacheUtil.delete(userProfileCacheKey);
            log.info("已删除用户信息和档案缓存: userId={}", userId);
            
            return Result.ok("删除成功");
    }
    
    /**
     * 查询用户列表（管理端内部接口）
     */
    @GetMapping("/users")
    public Result listUsers(@RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String accountStatus,
                           @RequestParam(required = false) String sortProp,
                           @RequestParam(required = false) String sortOrder) {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(0, page - 1), size,
                org.springframework.data.domain.Sort.by(
                    "desc".equalsIgnoreCase(sortOrder) ? 
                        org.springframework.data.domain.Sort.Direction.DESC : 
                        org.springframework.data.domain.Sort.Direction.ASC,
                    StringUtils.hasText(sortProp) ? sortProp : "registerTime"
                )
            );
            
            org.springframework.data.jpa.domain.Specification<User> spec = (root, query, cb) -> {
                java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
                
                if (StringUtils.hasText(keyword)) {
                    String kw = keyword.trim();
                    predicates.add(cb.or(
                        cb.like(root.get("username"), "%" + kw + "%"),
                        cb.like(root.get("primaryPhone"), "%" + kw + "%"),
                        cb.like(root.get("userId"), "%" + kw + "%")
                    ));
                }
                
                if (StringUtils.hasText(accountStatus)) {
                    predicates.add(cb.equal(root.get("accountStatus"), accountStatus.trim()));
                }
                
                return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            
            Page<User> userPage = userRepository.findAll(spec, pageable);
            
            // 转换为内部 DTO 列表
            List<UserInternalDTO> userDTOs = userInternalDTOConverter.toUserInternalDTOList(userPage.getContent());
            
            // 构建分页结果
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("content", userDTOs);
            result.put("totalElements", userPage.getTotalElements());
            result.put("totalPages", userPage.getTotalPages());
            result.put("number", userPage.getNumber());
            result.put("size", userPage.getSize());
            
            return Result.ok("查询成功", result);
    }
    
    /**
     * 设置订单提醒状态（内部接口，供Order Service调用）
     */
    @PutMapping("/user/{userId}/order-reminder")
    public Result setOrderReminderStatus(@PathVariable String userId,
                                        @RequestParam String role,
                                        @RequestParam Boolean hasNew) {
            userProfileService.setOrderReminderStatus(userId, role, hasNew);
            return Result.ok("设置成功");
    }
    
    /**
     * 根据地址ID获取地址信息（内部接口，供Order Service调用）
     */
    @GetMapping("/address/{addressId}")
    public Result getAddressById(@PathVariable String addressId) {
        UserAddress address = userAddressRepository.findById(addressId)
            .orElseThrow(() -> new BusinessException("地址不存在"));
        
        AddressInternalDTO dto = convertToAddressInternalDTO(address);
        return Result.ok("查询成功", dto);
    }
    
    /**
     * 获取用户的默认地址（内部接口，供Order Service调用）
     */
    @GetMapping("/address/default")
    public Result getDefaultAddress(@RequestParam String userId) {
        UserAddress address = userAddressRepository
            .findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
            .orElse(null);
        
        if (address == null) {
            return Result.ok("查询成功", null);
        }
        
        AddressInternalDTO dto = convertToAddressInternalDTO(address);
        return Result.ok("查询成功", dto);
    }
    
    /**
     * 生成用户画像向量（内部接口）
     * 单次 curl 即可生成画像，整合多个数据源
     * @param userId 用户ID
     * @return 生成结果
     */
    @PostMapping("/user/{userId}/generate-profile-vector")
    public Result generateUserProfileVector(@PathVariable String userId) {
        try {
            log.info("开始生成用户画像向量: userId={}", userId);
            userProfileVectorService.generateAndStoreUserProfileVector(userId);
            return Result.ok("用户画像向量生成成功（异步处理中）");
        } catch (Exception e) {
            log.error("生成用户画像向量失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("生成用户画像向量失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户画像向量（内部接口）
     * 用于商品推荐和搜索
     */
    @GetMapping("/user/{userId}/profile-vector")
    public Result getUserProfileVector(@PathVariable String userId) {
        try {
            java.util.List<Double> vector = userProfileVectorService.getUserProfileVector(userId);
            if (vector == null || vector.isEmpty()) {
                return Result.ok("用户画像向量不存在", null);
            }
            return Result.ok("查询成功", vector);
        } catch (Exception e) {
            log.error("获取用户画像向量失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("获取用户画像向量失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除用户画像向量（内部接口）
     * 用于管理端删除用户画像
     */
    @DeleteMapping("/user/{userId}/profile-vector")
    public Result deleteUserProfileVector(@PathVariable String userId) {
        try {
            log.info("删除用户画像向量: userId={}", userId);
            userProfileVectorService.deleteUserProfileVector(userId);
            return Result.ok("用户画像向量删除成功");
        } catch (Exception e) {
            log.error("删除用户画像向量失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("删除用户画像向量失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索相似用户（基于用户画像向量）
     * @param queryVector 查询向量（逗号分隔的浮点数）
     * @param limit 返回数量限制
     * @return 相似用户ID列表
     */
    @GetMapping("/user/search-similar")
    public Result searchSimilarUsers(@RequestParam String queryVector, 
                                    @RequestParam(defaultValue = "10") Integer limit) {
        try {
            // 解析向量字符串
            java.util.List<Double> vector = java.util.Arrays.stream(queryVector.split(","))
                .map(String::trim)
                .map(Double::parseDouble)
                .collect(java.util.stream.Collectors.toList());
            
            java.util.List<String> similarUserIds = userProfileVectorService.searchSimilarUsers(vector, limit);
            return Result.ok("搜索成功", similarUserIds);
        } catch (Exception e) {
            log.error("搜索相似用户失败: error={}", e.getMessage(), e);
            return Result.fail("搜索相似用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 转换地址实体为内部DTO
     */
    private AddressInternalDTO convertToAddressInternalDTO(UserAddress address) {
        AddressInternalDTO dto = new AddressInternalDTO();
        dto.setAddressId(address.getAddressId());
        dto.setUserId(address.getUserId());
        dto.setRecipientName(address.getRecipientName());
        dto.setRecipientPhone(address.getRecipientPhone());
        dto.setProvince(address.getProvince());
        dto.setCity(address.getCity());
        dto.setDistrict(address.getDistrict());
        dto.setStreetAddress(address.getStreetAddress());
        dto.setDetailAddress(address.getDetailAddress());
        dto.setFullAddress(address.getFullAddress());
        dto.setLongitude(address.getLongitude());
        dto.setLatitude(address.getLatitude());
        dto.setAddressLabel(address.getAddressLabel());
        dto.setIsDefault(address.getIsDefault());
        dto.setIsActive(address.getIsActive());
        return dto;
    }
}
