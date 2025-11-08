package com.njumarket.auth.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.InternalDTOConverter;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.UserProfile;
import com.njumarket.auth.repository.UserRepository;
import com.njumarket.auth.repository.UserProfileRepository;
import com.njumarket.auth.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final UserProfileService userProfileService;
    private final InternalDTOConverter internalDTOConverter;
    
    /**
     * 根据ID查询用户（内部接口）
     * 返回内部 DTO，不包含关联对象
     */
    @GetMapping("/user/{userId}")
    public Result getUserById(@PathVariable String userId) {
        try {
            User user = userRepository.findById(userId)
                .orElse(null);
            if (user == null) {
                return Result.fail("用户不存在");
            }
            
            // 调试：打印用户状态信息
            log.info("auth-service查询用户: userId={}, accountStatus=[{}], accountStatus是否为null={}", 
                user.getUserId(), user.getAccountStatus(), user.getAccountStatus() == null);
            
            UserInternalDTO dto = internalDTOConverter.toInternalDTO(user);
            
            // 调试：打印DTO状态信息
            log.info("auth-service返回UserInternalDTO: userId={}, accountStatus=[{}], accountStatus是否为null={}", 
                dto.getUserId(), dto.getAccountStatus(), dto.getAccountStatus() == null);
            
            return Result.ok("查询成功", dto);
        } catch (Exception e) {
            log.error("查询用户失败: {}", e.getMessage(), e);
            return Result.fail("查询用户失败");
        }
    }
    
    /**
     * 批量查询用户（内部接口）
     * 返回内部 DTO 列表，不包含关联对象
     */
    @GetMapping("/user/batch")
    public Result getUsersByIds(@RequestParam List<String> userIds) {
        try {
            List<User> users = userRepository.findAllById(userIds);
            List<UserInternalDTO> dtos = internalDTOConverter.toUserInternalDTOList(users);
            return Result.ok("批量查询成功", dtos);
        } catch (Exception e) {
            log.error("批量查询用户失败: {}", e.getMessage(), e);
            return Result.fail("批量查询用户失败");
        }
    }
    
    /**
     * 批量查询用户档案（内部接口）
     * 返回内部 DTO 列表，不包含关联对象
     */
    @GetMapping("/user/profile/batch")
    public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
        try {
            List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
            List<UserProfileInternalDTO> dtos = internalDTOConverter.toUserProfileInternalDTOList(profiles);
            return Result.ok("批量查询成功", dtos);
        } catch (Exception e) {
            log.error("批量查询用户档案失败: {}", e.getMessage(), e);
            return Result.fail("批量查询用户档案失败");
        }
    }
    
    /**
     * 更新用户完整信息（管理端内部接口）
     */
    @PutMapping("/user/{userId}/full")
    public Result updateUserFull(@PathVariable String userId, @RequestBody Map<String, Object> payload) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return Result.fail("用户不存在");
            }
            User user = userOpt.get();
            
            // 更新基本字段
            Object username = payload.get("username");
            if (username instanceof String && StringUtils.hasText((String) username)) {
                user.setUsername(((String) username).trim());
            }
            Object primaryPhone = payload.get("primaryPhone");
            if (primaryPhone instanceof String && StringUtils.hasText((String) primaryPhone)) {
                user.setPrimaryPhone(((String) primaryPhone).trim());
            }
            Object accountStatus = payload.get("accountStatus");
            if (accountStatus instanceof String && StringUtils.hasText((String) accountStatus)) {
                String newStatus = ((String) accountStatus).trim();
                java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList("ACTIVE","SUSPENDED","BANNED"));
                if (allowed.contains(newStatus)) {
                    user.setAccountStatus(newStatus);
                }
            }
            
            // 更新档案字段
            UserProfile profile = user.getUserProfile();
            if (profile == null) {
                profile = new UserProfile();
                profile.setProfileId("PROFILE_" + System.currentTimeMillis());
                profile.setUserId(user.getUserId());
            }
            Object nickname = payload.get("nickname");
            if (nickname instanceof String) profile.setNickname(((String) nickname).trim());
            Object avatar = payload.get("avatar");
            if (avatar instanceof String) profile.setAvatar(((String) avatar).trim());
            
            userRepository.save(user);
            userProfileRepository.save(profile);
            
            return Result.ok("更新成功");
        } catch (Exception e) {
            log.error("更新用户完整信息失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("更新失败");
        }
    }
    
    /**
     * 删除用户（管理端内部接口）
     */
    @DeleteMapping("/user/{userId}")
    public Result deleteUser(@PathVariable String userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return Result.fail("用户不存在");
            }
            User user = userOpt.get();
            user.setAccountStatus("DELETED");
            userRepository.save(user);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除用户失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("删除失败");
        }
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
        try {
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
            List<UserInternalDTO> userDTOs = internalDTOConverter.toUserInternalDTOList(userPage.getContent());
            
            // 构建分页结果
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("content", userDTOs);
            result.put("totalElements", userPage.getTotalElements());
            result.put("totalPages", userPage.getTotalPages());
            result.put("number", userPage.getNumber());
            result.put("size", userPage.getSize());
            
            return Result.ok("查询成功", result);
        } catch (Exception e) {
            log.error("查询用户列表失败: error={}", e.getMessage(), e);
            return Result.fail("查询失败");
        }
    }
    
    /**
     * 设置订单提醒状态（内部接口，供Order Service调用）
     */
    @PutMapping("/user/{userId}/order-reminder")
    public Result setOrderReminderStatus(@PathVariable String userId,
                                        @RequestParam String role,
                                        @RequestParam Boolean hasNew) {
        try {
            userProfileService.setOrderReminderStatus(userId, role, hasNew);
            return Result.ok("设置成功");
        } catch (Exception e) {
            log.error("设置订单提醒状态失败: userId={}, role={}, hasNew={}, error={}", 
                userId, role, hasNew, e.getMessage(), e);
            return Result.fail("设置失败");
        }
    }
}
