package com.njumarket.admin.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.AdminLoginDTO;
import com.njumarket.njumarket.entity.Admin;
import com.njumarket.njumarket.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.njumarket.entity.Conversation;
import com.njumarket.njumarket.entity.Message;
import com.njumarket.admin.repository.AdminRepository;
import com.njumarket.admin.service.AdminService;
import com.njumarket.admin.service.PasswordService;
import com.njumarket.njumarket.utils.JwtUtils;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.admin.client.AuthClient;
import com.njumarket.admin.client.CommodityClient;
import com.njumarket.admin.client.OrderClient;
import com.njumarket.admin.client.MessageClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理员服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final PasswordService passwordService;
    private final JwtUtils jwtUtils;
    private final AuthClient authClient;
    private final CommodityClient commodityClient;
    private final OrderClient orderClient;
    private final MessageClient messageClient;
    private final ObjectMapper objectMapper;

    @Override
    public Result login(AdminLoginDTO loginDTO, HttpSession session) {
        try {
            // 1. 参数验证
            if (!StringUtils.hasText(loginDTO.getUsername())) {
                return Result.fail("用户名不能为空");
            }
            if (!StringUtils.hasText(loginDTO.getPassword())) {
                return Result.fail("密码不能为空");
            }

            // 2. 查找管理员
            Optional<Admin> adminOpt = adminRepository.findByUsername(loginDTO.getUsername().trim());
            if (adminOpt.isEmpty()) {
                log.warn("error in username, 管理员登录失败: 用户名不存在, username={}", loginDTO.getUsername());
                return Result.fail("用户名或密码错误");
            }

            Admin admin = adminOpt.get();

            // 3. 检查账户状态
            if (!admin.canLogin()) {
                log.warn("管理员登录失败: 账户被禁用, username={}", loginDTO.getUsername());
                return Result.fail("账户已被禁用，请联系系统管理员");
            }

            // 4. 验证密码
            if (!passwordService.matches(loginDTO.getPassword(), admin.getPassword())) {
                log.warn("error in password, 管理员登录失败: 密码错误, username={}", loginDTO.getUsername());
                return Result.fail("用户名或密码错误");
            }

            // 5. 更新登录信息
            String clientIp = "127.0.0.1"; // 暂时使用默认IP，后续可以从request中获取
            admin.updateLoginInfo(clientIp);
            adminRepository.save(admin);

            // 6. 生成Token
            Map<String, Object> tokenResult = generateAndStoreTokens(admin);

            // 7. 存储到Session
            session.setAttribute("admin", admin);
            session.setAttribute("adminId", admin.getAdminId());

            log.info("管理员登录成功: adminId={}, username={}, ip={}", 
                admin.getAdminId(), admin.getUsername(), clientIp);

            return Result.ok(tokenResult);

        } catch (Exception e) {
            log.error("管理员登录异常: username={}, error={}", loginDTO.getUsername(), e.getMessage());
            return Result.fail("登录失败，请稍后重试");
        }
    }

    @Override
    public Result logout(HttpSession session) {
        try {
            Admin admin = (Admin) session.getAttribute("admin");
            if (admin != null) {
                log.info("管理员登出: adminId={}, username={}", admin.getAdminId(), admin.getUsername());
            }
            
            session.removeAttribute("admin");
            session.removeAttribute("adminId");
            session.invalidate();
            
            return Result.ok("登出成功");
        } catch (Exception e) {
            log.error("管理员登出异常: {}", e.getMessage());
            return Result.fail("登出失败");
        }
    }

    @Override
    public Result getCurrentAdmin() {
        try {
            Admin admin = SecurityUtils.requireCurrentAdmin();
            
            // 不返回密码等敏感信息
            Map<String, Object> adminInfo = new HashMap<>();
            adminInfo.put("adminId", admin.getAdminId());
            adminInfo.put("username", admin.getUsername());
            adminInfo.put("realName", admin.getRealName());
            adminInfo.put("email", admin.getEmail());
            adminInfo.put("department", admin.getDepartment());
            adminInfo.put("position", admin.getPosition());
            adminInfo.put("adminLevel", admin.getAdminLevel());
            adminInfo.put("permissions", admin.getPermissions());
            adminInfo.put("lastLoginTime", admin.getLastLoginTime());
            adminInfo.put("lastLoginIp", admin.getLastLoginIp());
            adminInfo.put("loginCount", admin.getLoginCount());
            adminInfo.put("accountStatus", admin.getAccountStatus());
            adminInfo.put("createTime", admin.getCreateTime());
            
            return Result.ok(adminInfo);
        } catch (Exception e) {
            log.error("获取当前管理员信息异常: {}", e.getMessage());
            return Result.fail("获取管理员信息失败");
        }
    }

    @Override
    public Result createAdmin(Admin admin) {
        try {
            // ✅ 权限检查：只有system权限的管理员才能创建管理员
            Admin currentAdmin = com.njumarket.njumarket.utils.SecurityUtils.requireCurrentAdmin();
            if (!currentAdmin.isSystemAdmin()) {
                log.warn("非system权限管理员尝试创建管理员: adminId={}", 
                    currentAdmin.getAdminId());
                return Result.fail("权限不足，只有system权限的管理员才能创建管理员");
            }

            // 1. 参数验证
            if (!StringUtils.hasText(admin.getUsername())) {
                return Result.fail("用户名不能为空");
            }
            if (!StringUtils.hasText(admin.getPassword())) {
                return Result.fail("密码不能为空");
            }
            if (admin.getPassword().length() < 6) {
                return Result.fail("密码长度不能少于6位");
            }

            // 2. 检查用户名是否已存在
            if (adminRepository.existsByUsername(admin.getUsername())) {
                return Result.fail("用户名已存在");
            }

            // ✅ 3. 限制：新创建的管理员级别只能是administrator，不能创建system管理员
            if (admin.getAdminLevel() != null && "system".equals(admin.getAdminLevel())) {
                return Result.fail("不允许创建system权限的管理员");
            }

            // 4. 设置默认值
            if (admin.getAdminId() == null) {
                admin.setAdminId("ADMIN_" + System.currentTimeMillis());
            }
            // ✅ 强制设置为administrator级别
            admin.setAdminLevel("administrator");
            admin.setPassword(passwordService.encodePassword(admin.getPassword()));
            admin.setAccountStatus("ACTIVE");
            admin.setCreateTime(LocalDateTime.now());
            admin.setUpdateTime(LocalDateTime.now());

            // 5. 保存管理员
            Admin savedAdmin = adminRepository.save(admin);

            log.info("创建管理员成功: adminId={}, username={}, operatorId={}", 
                savedAdmin.getAdminId(), savedAdmin.getUsername(), currentAdmin.getAdminId());

            Map<String, Object> result = toSimpleAdmin(savedAdmin);
            return Result.ok("创建管理员成功", result);

        } catch (Exception e) {
            log.error("创建管理员异常: username={}, error={}", admin.getUsername(), e.getMessage(), e);
            return Result.fail("创建管理员失败：" + e.getMessage());
        }
    }

    @Override
    public Result updateAdmin(String adminId, Admin admin) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }

            Admin existingAdmin = adminOpt.get();

            // 更新允许修改的字段
            if (StringUtils.hasText(admin.getRealName())) {
                existingAdmin.setRealName(admin.getRealName());
            }
            if (StringUtils.hasText(admin.getEmail())) {
                existingAdmin.setEmail(admin.getEmail());
            }
            if (StringUtils.hasText(admin.getDepartment())) {
                existingAdmin.setDepartment(admin.getDepartment());
            }
            if (StringUtils.hasText(admin.getPosition())) {
                existingAdmin.setPosition(admin.getPosition());
            }
            if (StringUtils.hasText(admin.getAdminLevel())) {
                existingAdmin.setAdminLevel(admin.getAdminLevel());
            }
            if (StringUtils.hasText(admin.getPermissions())) {
                existingAdmin.setPermissions(admin.getPermissions());
            }
            if (StringUtils.hasText(admin.getRemark())) {
                existingAdmin.setRemark(admin.getRemark());
            }

            existingAdmin.setUpdateTime(LocalDateTime.now());

            Admin updatedAdmin = adminRepository.save(existingAdmin);

            log.info("更新管理员成功: adminId={}, username={}", updatedAdmin.getAdminId(), updatedAdmin.getUsername());

            return Result.ok(updatedAdmin);

        } catch (Exception e) {
            log.error("更新管理员异常: adminId={}, error={}", adminId, e.getMessage());
            return Result.fail("更新管理员失败");
        }
    }

    @Override
    public Result deleteAdmin(String adminId) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }

            Admin admin = adminOpt.get();
            
            // 检查是否为系统管理员
            if (admin.isSystemAdmin()) {
                return Result.fail("不能删除系统管理员");
            }

            adminRepository.deleteById(adminId);

            log.info("删除管理员成功: adminId={}, username={}", admin.getAdminId(), admin.getUsername());

            return Result.ok("删除成功");

        } catch (Exception e) {
            log.error("删除管理员异常: adminId={}, error={}", adminId, e.getMessage());
            return Result.fail("删除管理员失败");
        }
    }

    @Override
    public Result getAdminList(Integer page, Integer size, String keyword, String accountStatus, String sortProp, String sortOrder) {
        try {
            // ✅ 权限检查：只有system权限的管理员才能查看管理员列表
            Admin currentAdmin = com.njumarket.njumarket.utils.SecurityUtils.requireCurrentAdmin();
            if (!currentAdmin.isSystemAdmin()) {
                log.warn("非system权限管理员尝试访问管理员列表: adminId={}", 
                    currentAdmin.getAdminId());
                return Result.fail("权限不足，只有system权限的管理员才能查看管理员列表");
            }

            String kw = keyword == null ? "" : keyword.trim().toLowerCase();
            org.springframework.data.jpa.domain.Specification<Admin> spec = (root, query, cb) -> {
                java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
                
                // ✅ 关键词搜索：支持按用户名、真实姓名、邮箱搜索
                if (!kw.isEmpty()) {
                    predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("realName")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("email")), "%" + kw + "%")
                    ));
                }
                
                // ✅ 账户状态筛选
                if (org.springframework.util.StringUtils.hasText(accountStatus)) {
                    predicates.add(cb.equal(root.get("accountStatus"), accountStatus.trim()));
                }
                
                return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };

            // ✅ 排序（默认 createTime desc）
            Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
            if (org.springframework.util.StringUtils.hasText(sortProp)) {
                String sp = sortProp.trim();
                Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
                
                // ✅ 特殊处理：lastLoginTime排序需要考虑null值（未登录的管理员）
                if ("lastLoginTime".equals(sp)) {
                    sort = Sort.by(dir, "lastLoginTime");
                } else if ("createTime".equals(sp)) {
                    sort = Sort.by(dir, "createTime");
                }
            }

            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);
            Page<Admin> adminPage = adminRepository.findAll(spec, pageable);

            // ✅ 移除密码字段，避免返回敏感信息
            List<Map<String, Object>> adminList = new ArrayList<>();
            for (Admin admin : adminPage.getContent()) {
                Map<String, Object> adminMap = toSimpleAdmin(admin);
                adminList.add(adminMap);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("list", adminList);
            result.put("total", adminPage.getTotalElements());
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", adminPage.getTotalPages());

            return Result.ok(result);

        } catch (Exception e) {
            log.error("获取管理员列表异常: error={}", e.getMessage(), e);
            return Result.fail("获取管理员列表失败");
        }
    }

    @Override
    public Result getAdminById(String adminId) {
        try {
            // ✅ 权限检查：只有system权限的管理员才能查看其他管理员信息
            Admin currentAdmin = com.njumarket.njumarket.utils.SecurityUtils.requireCurrentAdmin();
            if (!currentAdmin.isSystemAdmin()) {
                log.warn("非system权限管理员尝试查看管理员信息: adminId={}, targetAdminId={}", 
                    currentAdmin.getAdminId(), adminId);
                return Result.fail("权限不足，只有system权限的管理员才能查看管理员信息");
            }

            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }

            Admin admin = adminOpt.get();
            // ✅ 返回简化的管理员信息（不包含密码）
            Map<String, Object> adminMap = toSimpleAdmin(admin);

            return Result.ok(adminMap);

        } catch (Exception e) {
            log.error("获取管理员信息异常: adminId={}, error={}", adminId, e.getMessage());
            return Result.fail("获取管理员信息失败");
        }
    }

    @Override
    public Result updateAdminStatus(String adminId, String status) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }

            Admin admin = adminOpt.get();
            admin.setAccountStatus(status);
            admin.setUpdateTime(LocalDateTime.now());

            adminRepository.save(admin);

            log.info("更新管理员状态成功: adminId={}, status={}", adminId, status);

            return Result.ok("状态更新成功");

        } catch (Exception e) {
            log.error("更新管理员状态异常: adminId={}, status={}, error={}", adminId, status, e.getMessage());
            return Result.fail("更新状态失败");
        }
    }

    @Override
    public Result resetPassword(String adminId, String newPassword) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }

            Admin admin = adminOpt.get();
            admin.setPassword(passwordService.encodePassword(newPassword));
            admin.setUpdateTime(LocalDateTime.now());

            adminRepository.save(admin);

            log.info("重置管理员密码成功: adminId={}", adminId);

            return Result.ok("密码重置成功");

        } catch (Exception e) {
            log.error("重置管理员密码异常: adminId={}, error={}", adminId, e.getMessage());
            return Result.fail("密码重置失败");
        }
    }

    @Override
    public Result changePassword(String adminId, String oldPassword, String newPassword) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }

            Admin admin = adminOpt.get();

            // 验证旧密码
            if (!passwordService.matches(oldPassword, admin.getPassword())) {
                return Result.fail("原密码错误");
            }

            // 更新密码
            admin.setPassword(passwordService.encodePassword(newPassword));
            admin.setUpdateTime(LocalDateTime.now());

            adminRepository.save(admin);

            log.info("修改管理员密码成功: adminId={}", adminId);

            return Result.ok("密码修改成功");

        } catch (Exception e) {
            log.error("修改管理员密码异常: adminId={}, error={}", adminId, e.getMessage());
            return Result.fail("密码修改失败");
        }
    }

    @Override
    public Result getAdminStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 总管理员数
            long totalAdmins = adminRepository.count();
            statistics.put("totalAdmins", totalAdmins);
            
            // 活跃管理员数
            long activeAdmins = adminRepository.countActiveAdmins();
            statistics.put("activeAdmins", activeAdmins);
            
            // 各级别管理员数量
            List<Object[]> levelCounts = adminRepository.countAdminsByLevel();
            Map<String, Long> levelStats = new HashMap<>();
            for (Object[] levelCount : levelCounts) {
                levelStats.put((String) levelCount[0], (Long) levelCount[1]);
            }
            statistics.put("levelStats", levelStats);
            
            // 系统管理员数量
            long systemAdmins = adminRepository.findByAdminLevel("system").size();
            statistics.put("systemAdmins", systemAdmins);
            
            // 普通管理员数量
            long administratorCount = adminRepository.findByAdminLevel("administrator").size();
            statistics.put("administratorCount", administratorCount);
            
            return Result.ok(statistics);

        } catch (Exception e) {
            log.error("获取管理员统计信息异常: error={}", e.getMessage());
            return Result.fail("获取统计信息失败");
        }
    }

    @Override
    public Result checkPermission(String adminId, String permission) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }

            Admin admin = adminOpt.get();
            boolean hasPermission = admin.hasPermission(permission);

            Map<String, Object> result = new HashMap<>();
            result.put("adminId", adminId);
            result.put("permission", permission);
            result.put("hasPermission", hasPermission);

            return Result.ok(result);

        } catch (Exception e) {
            log.error("检查管理员权限异常: adminId={}, permission={}, error={}", adminId, permission, e.getMessage());
            return Result.fail("权限检查失败");
        }
    }

    @Override
    public Result updateAdminFull(String adminId, java.util.Map<String, Object> payload) {
        try {
            // ✅ 权限检查：只有system权限的管理员才能更新其他管理员信息
            Admin currentAdmin = com.njumarket.njumarket.utils.SecurityUtils.requireCurrentAdmin();
            if (!currentAdmin.isSystemAdmin()) {
                log.warn("非system权限管理员尝试更新管理员信息: adminId={}, targetAdminId={}", 
                    currentAdmin.getAdminId(), adminId);
                return Result.fail("权限不足，只有system权限的管理员才能更新管理员信息");
            }

            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }

            Admin admin = adminOpt.get();

            // ✅ 更新所有非客观字段（不包括createTime、updateTime、lastLoginTime、loginCount等）
            if (payload.containsKey("username")) {
                String username = String.valueOf(payload.get("username")).trim();
                if (username.isEmpty()) {
                    return Result.fail("用户名不能为空");
                }
                // 检查用户名是否已被其他管理员使用
                Optional<Admin> existingAdmin = adminRepository.findByUsername(username);
                if (existingAdmin.isPresent() && !existingAdmin.get().getAdminId().equals(adminId)) {
                    return Result.fail("用户名已被使用");
                }
                admin.setUsername(username);
            }

            if (payload.containsKey("password")) {
                // ✅ 更新密码
                String newPassword = String.valueOf(payload.get("password")).trim();
                if (newPassword.isEmpty()) {
                    return Result.fail("密码不能为空");
                }
                if (newPassword.length() < 6) {
                    return Result.fail("密码长度不能少于6位");
                }
                admin.setPassword(passwordService.encodePassword(newPassword));
            }

            if (payload.containsKey("realName")) {
                admin.setRealName(payload.get("realName") != null ? String.valueOf(payload.get("realName")).trim() : null);
            }

            if (payload.containsKey("email")) {
                admin.setEmail(payload.get("email") != null ? String.valueOf(payload.get("email")).trim() : null);
            }

            if (payload.containsKey("department")) {
                admin.setDepartment(payload.get("department") != null ? String.valueOf(payload.get("department")).trim() : null);
            }

            if (payload.containsKey("position")) {
                admin.setPosition(payload.get("position") != null ? String.valueOf(payload.get("position")).trim() : null);
            }

            // ✅ 管理员级别为固定字段，不允许修改
            if (payload.containsKey("adminLevel")) {
                return Result.fail("管理员级别为固定字段，不允许修改");
            }

            if (payload.containsKey("permissions")) {
                Object perms = payload.get("permissions");
                if (perms instanceof List) {
                    String permissionsJson = String.join(",", (List<String>) perms);
                    admin.setPermissions(permissionsJson);
                } else if (perms instanceof String) {
                    admin.setPermissions((String) perms);
                }
            }

            if (payload.containsKey("accountStatus")) {
                String status = String.valueOf(payload.get("accountStatus")).trim();
                if (!"ACTIVE".equals(status) && !"SUSPENDED".equals(status) && !"BANNED".equals(status)) {
                    return Result.fail("无效的账户状态");
                }
                
                // ✅ 禁止修改系统管理员的账户状态
                if ("system".equals(admin.getAdminLevel())) {
                    return Result.fail("系统管理员的账户状态不允许修改");
                }
                
                admin.setAccountStatus(status);
            }

            if (payload.containsKey("remark")) {
                admin.setRemark(payload.get("remark") != null ? String.valueOf(payload.get("remark")).trim() : null);
            }

            // updateTime 由 @UpdateTimestamp 自动更新，不需要手动设置
            adminRepository.save(admin);

            log.info("更新管理员信息成功: adminId={}, operatorId={}", adminId, currentAdmin.getAdminId());

            Map<String, Object> result = toSimpleAdmin(admin);
            return Result.ok("更新成功", result);

        } catch (Exception e) {
            log.error("更新管理员信息异常: adminId={}, error={}", adminId, e.getMessage(), e);
            return Result.fail("更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result updatePermissions(String adminId, List<String> permissions) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }

            Admin admin = adminOpt.get();
            
            // 将权限列表转换为JSON字符串
            String permissionsJson = String.join(",", permissions);
            admin.setPermissions(permissionsJson);
            admin.setUpdateTime(LocalDateTime.now());

            adminRepository.save(admin);

            log.info("更新管理员权限成功: adminId={}, permissions={}", adminId, permissions);

            return Result.ok("权限更新成功");

        } catch (Exception e) {
            log.error("更新管理员权限异常: adminId={}, permissions={}, error={}", adminId, permissions, e.getMessage());
            return Result.fail("权限更新失败");
        }
    }

    /**
     * 生成并存储Token
     */
    private Map<String, Object> generateAndStoreTokens(Admin admin) {
        // 生成JWT Token
        String token = jwtUtils.generateToken(admin.getAdminId(), admin.getUsername());
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("adminId", admin.getAdminId());
        result.put("username", admin.getUsername());
        result.put("adminLevel", admin.getAdminLevel());
        result.put("expiresIn", 24 * 60 * 60); // 24小时
        
        return result;
    }

    // ===================== 管理端最小CRUD：用户 =====================
    @Override
    public Result listUsers(Integer page, Integer size, String keyword, String accountStatus, String sortProp, String sortOrder) {
        try {
            // 通过Feign Client调用auth-service获取用户列表
            Result usersResult = authClient.listUsers(page, size, keyword, accountStatus, sortProp, sortOrder);
            if (!usersResult.getSuccess() || usersResult.getData() == null) {
                return Result.fail(usersResult.getErrorMsg() != null ? usersResult.getErrorMsg() : "获取用户列表失败");
            }
            
            // ✅ 使用ObjectMapper正确转换Page对象（避免ClassCastException）
            org.springframework.data.domain.Page<User> userPage;
            try {
                // Page对象需要特殊处理，先转换为Map，然后手动构建Page
                @SuppressWarnings("unchecked")
                Map<String, Object> pageMap = (Map<String, Object>) usersResult.getData();
                if (pageMap == null) {
                    return Result.fail("获取用户列表失败");
                }
                
                // 转换content列表
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) pageMap.get("content");
                List<User> users = new ArrayList<>();
                if (contentList != null) {
                    for (Map<String, Object> userMap : contentList) {
                        try {
                            UserInternalDTO userDTO = objectMapper.convertValue(
                                userMap,
                                new TypeReference<UserInternalDTO>() {}
                            );
                            users.add(convertUserDTOToEntity(userDTO));
                        } catch (Exception e) {
                            log.warn("转换用户失败，跳过: error={}", e.getMessage());
                        }
                    }
                }
                
                // 构建Page对象
                int pageNumber = pageMap.get("number") != null ? ((Number) pageMap.get("number")).intValue() : 0;
                int pageSize = pageMap.get("size") != null ? ((Number) pageMap.get("size")).intValue() : size;
                long totalElements = pageMap.get("totalElements") != null ? ((Number) pageMap.get("totalElements")).longValue() : users.size();
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(pageNumber, pageSize);
                userPage = new org.springframework.data.domain.PageImpl<>(users, pageable, totalElements);
            } catch (Exception e) {
                log.error("转换Page<User>失败: error={}", e.getMessage(), e);
                return Result.fail("用户列表解析失败");
            }
            
            // ✅ 批量查询所有用户的 UserProfile（避免 N+1 查询）
            List<User> users = userPage.getContent();
            Set<String> userIds = users.stream()
                    .map(User::getUserId)
                    .collect(java.util.stream.Collectors.toSet());
            
            Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                Result profilesResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
                if (profilesResult.getSuccess() && profilesResult.getData() != null) {
                    try {
                        List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                            profilesResult.getData(),
                            new TypeReference<List<UserProfileInternalDTO>>() {}
                        );
                    profileMap = profiles.stream()
                            .collect(java.util.stream.Collectors.toMap(UserProfileInternalDTO::getUserId, profile -> profile));
                    } catch (Exception e) {
                        log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                    }
                }
            }
            
            // ✅ 转换为包含用户信息的简单对象
            final Map<String, UserProfileInternalDTO> finalProfileMap = profileMap;
            List<Map<String, Object>> simpleList = users.stream()
                    .map(user -> {
                        Map<String, Object> simpleUser = toSimpleUser(user);
                        // 添加Profile信息
                        UserProfileInternalDTO profile = finalProfileMap.get(user.getUserId());
                        if (profile != null) {
                            Map<String, Object> profileInfo = new HashMap<>();
                            profileInfo.put("nickname", profile.getNickname());
                            profileInfo.put("avatar", profile.getAvatar());
                            profileInfo.put("creditScore", profile.getCreditScore());
                            profileInfo.put("buyerRating", profile.getBuyerRating());
                            profileInfo.put("sellerRating", profile.getSellerRating());
                            simpleUser.put("profile", profileInfo);
                        }
                        return simpleUser;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", simpleList);
            result.put("total", userPage.getTotalElements());
            result.put("pages", userPage.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取用户列表异常: {}", e.getMessage(), e);
            return Result.fail("获取用户列表失败");
        }
    }

    @Override
    public Result getUserById(String userId) {
        try {
            Result result = authClient.getUserById(userId);
            if (!result.getSuccess() || result.getData() == null) {
                return Result.fail(result.getErrorMsg() != null ? result.getErrorMsg() : "用户不存在");
            }
            // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
            UserInternalDTO userDTO;
            try {
                userDTO = objectMapper.convertValue(
                    result.getData(),
                    new TypeReference<UserInternalDTO>() {}
                );
            } catch (Exception e) {
                log.error("转换User失败: userId={}, error={}", userId, e.getMessage(), e);
                return Result.fail("用户信息解析失败");
            }
            // 转换为User实体
            User user = convertUserDTOToEntity(userDTO);
            return Result.ok(toSimpleUser(user));
        } catch (Exception e) {
            log.error("获取用户信息异常: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("获取用户信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateUserStatus(String userId, String status) {
        try {
            return authClient.updateUserStatus(userId, status);
        } catch (Exception e) {
            log.error("更新用户状态异常: userId={}, status={}, error={}", userId, status, e.getMessage(), e);
            return Result.fail("更新用户状态失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateUserBasic(String userId, String nickname, String phone, String email) {
        try {
            return authClient.updateUserBasic(userId, nickname, phone, email);
        } catch (Exception e) {
            log.error("更新用户基础信息异常: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("更新用户信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result deleteUser(String userId) {
        try {
            return authClient.deleteUser(userId);
        } catch (Exception e) {
            log.error("删除用户异常: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("删除用户失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateUserFull(String userId, Map<String, Object> payload) {
        try {
            Result result = authClient.updateUserFull(userId, payload);
            if (!result.getSuccess()) {
                return result;
            }
            // 重新获取用户信息以返回完整数据
            Result userResult = authClient.getUserById(userId);
            if (!userResult.getSuccess() || userResult.getData() == null) {
                return Result.ok("更新成功");
            }
            // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
            UserInternalDTO userDTO;
            try {
                userDTO = objectMapper.convertValue(
                    userResult.getData(),
                    new TypeReference<UserInternalDTO>() {}
                );
            } catch (Exception e) {
                log.error("转换User失败: userId={}, error={}", userId, e.getMessage(), e);
                return Result.ok("更新成功");
            }
            // 转换为User实体
            User user = convertUserDTOToEntity(userDTO);
            // 更新逻辑已由auth-service的InternalController处理

            return Result.ok("更新成功", toSimpleUser(user));
        } catch (Exception e) {
            log.error("完整更新用户异常: userId={}, error={}", userId, e.getMessage());
            return Result.fail("更新失败");
        }
    }

    // ===================== 管理端最小CRUD：商品 =====================
    @Override
    public Result listCommodities(Integer page, Integer size, String keyword, String category, String conditionLevel, String status, String sellerVisibility, String buyerVisibility, String sortProp, String sortOrder) {
        // 注意：商品列表查询功能需要在commodity-service中添加管理端内部API
        // 暂时返回空列表
        Map<String, Object> result = new HashMap<>();
        result.put("list", new ArrayList<>());
        result.put("total", 0L);
        result.put("pages", 0);
        result.put("current", page);
        result.put("size", size);
        return Result.ok(result);
    }

    @Override
    public Result getCommodityById(String commodityId) {
        try {
            return commodityClient.getCommodityById(commodityId);
        } catch (Exception e) {
            log.error("获取商品信息异常: commodityId={}, error={}", commodityId, e.getMessage(), e);
            return Result.fail("获取商品信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateCommodityStatus(String commodityId, String status) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("commodityStatus", status);
            return commodityClient.updateCommodityFull(commodityId, payload);
        } catch (Exception e) {
            log.error("更新商品状态异常: commodityId={}, status={}, error={}", commodityId, status, e.getMessage(), e);
            return Result.fail("更新商品状态失败: " + e.getMessage());
        }
    }

    @Override
    public Result deleteCommodity(String commodityId) {
        try {
            return commodityClient.deleteCommodity(commodityId);
        } catch (Exception e) {
            log.error("删除商品异常: commodityId={}, error={}", commodityId, e.getMessage(), e);
            return Result.fail("删除商品失败: " + e.getMessage());
        }
    }

    // ===================== 管理端最小CRUD：订单 =====================
    @Override
    public Result listOrders(Integer page, Integer size, String keyword, String status, String sellerVisibility, String buyerVisibility, String sortProp, String sortOrder) {
        // 注意：订单列表查询功能需要在order-service中添加管理端内部API
        // 暂时返回空列表
        Map<String, Object> result = new HashMap<>();
        result.put("list", new ArrayList<>());
        result.put("total", 0L);
        result.put("pages", 0);
        result.put("current", page);
        result.put("size", size);
        return Result.ok(result);
    }

    @Override
    public Result getOrderById(String orderId) {
        try {
            return orderClient.getOrderById(orderId);
        } catch (Exception e) {
            log.error("获取订单信息异常: orderId={}, error={}", orderId, e.getMessage(), e);
            return Result.fail("获取订单信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateOrderFields(String orderId, String status, String trackingNumber, String remark) {
        try {
            Map<String, Object> payload = new HashMap<>();
            if (status != null) payload.put("orderStatus", status);
            if (trackingNumber != null) payload.put("trackingNumber", trackingNumber);
            if (remark != null) payload.put("remark", remark);
            return orderClient.updateOrderFull(orderId, payload);
        } catch (Exception e) {
            log.error("更新订单异常: orderId={}, error={}", orderId, e.getMessage(), e);
            return Result.fail("更新订单失败: " + e.getMessage());
        }
    }
    
    // ✅ 新增：更新订单完整字段（包括状态和可见性）
    @Override
    public Result updateOrderFull(String orderId, Map<String, Object> payload) {
        try {
            return orderClient.updateOrderFull(orderId, payload);
        } catch (Exception e) {
            log.error("完整更新订单异常: orderId={}, error={}", orderId, e.getMessage(), e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    @Override
    public Result deleteOrder(String orderId) {
        try {
            return orderClient.deleteOrder(orderId);
        } catch (Exception e) {
            log.error("删除订单异常: orderId={}, error={}", orderId, e.getMessage(), e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    // ===================== 管理端最小CRUD：会话/消息 =====================
    @Override
    public Result listConversations(Integer page, Integer size, String keyword) {
        try {
            Result conversationsResult = messageClient.listConversations(page, size, keyword);
            if (!conversationsResult.getSuccess() || conversationsResult.getData() == null) {
                return Result.fail(conversationsResult.getErrorMsg() != null ? conversationsResult.getErrorMsg() : "获取会话列表失败");
            }
            
            // ✅ 使用ObjectMapper正确转换Page对象（避免ClassCastException）
            org.springframework.data.domain.Page<Conversation> p;
            try {
                // Page对象需要特殊处理，先转换为Map，然后手动构建Page
                @SuppressWarnings("unchecked")
                Map<String, Object> pageMap = (Map<String, Object>) conversationsResult.getData();
                if (pageMap == null) {
                    return Result.fail("获取会话列表失败");
                }
                
                // 转换content列表
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) pageMap.get("content");
                List<Conversation> conversations = new ArrayList<>();
                if (contentList != null) {
                    for (Map<String, Object> convMap : contentList) {
                        try {
                            Conversation conv = objectMapper.convertValue(
                                convMap,
                                new TypeReference<Conversation>() {}
                            );
                            conversations.add(conv);
                        } catch (Exception e) {
                            log.warn("转换会话失败，跳过: error={}", e.getMessage());
                        }
                    }
                }
                
                // 构建Page对象
                int pageNumber = pageMap.get("number") != null ? ((Number) pageMap.get("number")).intValue() : 0;
                int pageSize = pageMap.get("size") != null ? ((Number) pageMap.get("size")).intValue() : size;
                long totalElements = pageMap.get("totalElements") != null ? ((Number) pageMap.get("totalElements")).longValue() : conversations.size();
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(pageNumber, pageSize);
                p = new org.springframework.data.domain.PageImpl<>(conversations, pageable, totalElements);
            } catch (Exception e) {
                log.error("转换Page<Conversation>失败: error={}", e.getMessage(), e);
                return Result.fail("会话列表解析失败");
            }
            
            // ✅ 批量查询所有用户的 UserProfile（避免 N+1 查询）
            List<Conversation> conversations = p.getContent();
            Set<String> userIds = new HashSet<>();
            for (Conversation c : conversations) {
                if (c.getUserId1() != null) userIds.add(c.getUserId1());
                if (c.getUserId2() != null) userIds.add(c.getUserId2());
            }
            
            Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                Result profilesResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
                if (profilesResult.getSuccess() && profilesResult.getData() != null) {
                    try {
                        List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                            profilesResult.getData(),
                            new TypeReference<List<UserProfileInternalDTO>>() {}
                        );
                    profileMap = profiles.stream()
                            .collect(java.util.stream.Collectors.toMap(UserProfileInternalDTO::getUserId, profile -> profile));
                    } catch (Exception e) {
                        log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                    }
                }
            }
            
            // ✅ 转换为包含用户信息的简单对象
            final Map<String, UserProfileInternalDTO> finalProfileMap = profileMap;
            List<Map<String, Object>> simpleList = conversations.stream()
                    .map(c -> toSimpleConversationWithUsers(c, 
                            finalProfileMap.get(c.getUserId1()), 
                            finalProfileMap.get(c.getUserId2())))
                    .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", simpleList);
            result.put("total", p.getTotalElements());
            result.put("pages", p.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取会话列表异常: {}", e.getMessage());
            return Result.fail("获取会话列表失败");
        }
    }

    @Override
    public Result getConversationById(String conversationId) {
        try {
            Result result = messageClient.getConversationById(conversationId);
            if (!result.getSuccess() || result.getData() == null) {
                return Result.fail(result.getErrorMsg() != null ? result.getErrorMsg() : "会话不存在");
            }
            // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
            Conversation c;
            try {
                c = objectMapper.convertValue(
                    result.getData(),
                    new TypeReference<Conversation>() {}
                );
            } catch (Exception e) {
                log.error("转换Conversation失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
                return Result.fail("会话信息解析失败");
            }
            
            // ✅ 批量查询用户Profile（避免N+1查询）
            Set<String> userIds = new HashSet<>();
            if (c.getUserId1() != null) userIds.add(c.getUserId1());
            if (c.getUserId2() != null) userIds.add(c.getUserId2());
            
            Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                Result profilesResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
                if (profilesResult.getSuccess() && profilesResult.getData() != null) {
                    try {
                        List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                            profilesResult.getData(),
                            new TypeReference<List<UserProfileInternalDTO>>() {}
                        );
                    profileMap = profiles.stream()
                            .collect(java.util.stream.Collectors.toMap(UserProfileInternalDTO::getUserId, profile -> profile));
                    } catch (Exception e) {
                        log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                    }
                }
            }
            
            Map<String, Object> simpleResult = toSimpleConversationWithUsers(c, 
                    profileMap.get(c.getUserId1()), 
                    profileMap.get(c.getUserId2()));
            return Result.ok(simpleResult);
        } catch (Exception e) {
            log.error("获取会话详情异常: conversationId={}, error={}", conversationId, e.getMessage(), e);
            return Result.fail("获取会话详情失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateConversationFull(String conversationId, java.util.Map<String, Object> payload) {
        try {
            // 调用message-service更新会话
            Result updateResult = messageClient.updateConversationFull(conversationId, payload);
            if (!updateResult.getSuccess()) {
                return updateResult;
            }
            return Result.ok("会话更新成功", updateResult.getData());
        } catch (Exception e) {
            log.error("完整更新会话异常: conversationId={}, error={}", conversationId, e.getMessage());
            return Result.fail("更新会话失败");
        }
    }

    @Override
    public Result deleteConversation(String conversationId) {
        try {
            return messageClient.deleteConversation(conversationId);
        } catch (Exception e) {
            log.error("删除会话异常: conversationId={}, error={}", conversationId, e.getMessage(), e);
            return Result.fail("删除会话失败: " + e.getMessage());
        }
    }

    @Override
    public Result listMessages(String conversationId, Integer page, Integer size) {
        try {
            Result result = messageClient.listMessages(conversationId, page, size);
            if (!result.getSuccess() || result.getData() == null) {
                return Result.fail(result.getErrorMsg() != null ? result.getErrorMsg() : "获取消息列表失败");
            }
            
            // ✅ 使用ObjectMapper正确转换Page对象（避免ClassCastException）
            org.springframework.data.domain.Page<Message> p;
            try {
                // Page对象需要特殊处理，先转换为Map，然后手动构建Page
                @SuppressWarnings("unchecked")
                Map<String, Object> pageMap = (Map<String, Object>) result.getData();
                if (pageMap == null) {
                    return Result.fail("获取消息列表失败");
                }
                
                // 转换content列表
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) pageMap.get("content");
                List<Message> messages = new ArrayList<>();
                if (contentList != null) {
                    for (Map<String, Object> msgMap : contentList) {
                        try {
                            Message msg = objectMapper.convertValue(
                                msgMap,
                                new TypeReference<Message>() {}
                            );
                            messages.add(msg);
                        } catch (Exception e) {
                            log.warn("转换消息失败，跳过: error={}", e.getMessage());
                        }
                    }
                }
                
                // 构建Page对象
                int pageNumber = pageMap.get("number") != null ? ((Number) pageMap.get("number")).intValue() : 0;
                int pageSize = pageMap.get("size") != null ? ((Number) pageMap.get("size")).intValue() : size;
                long totalElements = pageMap.get("totalElements") != null ? ((Number) pageMap.get("totalElements")).longValue() : messages.size();
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(pageNumber, pageSize);
                p = new org.springframework.data.domain.PageImpl<>(messages, pageable, totalElements);
            } catch (Exception e) {
                log.error("转换Page<Message>失败: error={}", e.getMessage(), e);
                return Result.fail("消息列表解析失败");
            }
            
            // 统计双方都删除的消息数量（用于日志和验证）
            long bothDeletedCount = 0;
            List<Map<String, Object>> simpleList = new ArrayList<>();
            for (Message m : p.getContent()) {
                // 统计双方都删除的消息
                if (Boolean.TRUE.equals(m.getDeletedBySender()) && Boolean.TRUE.equals(m.getDeletedByReceiver())) {
                    bothDeletedCount++;
                }
                simpleList.add(toSimpleMessage(m));
            }
            
            // 记录日志，验证是否查询到了双方都删除的消息
            log.debug("管理端查询消息列表: conversationId={}, page={}, size={}, total={}, bothDeletedCount={}", 
                    conversationId, page, size, p.getTotalElements(), bothDeletedCount);
            
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("list", simpleList);
            resultMap.put("total", p.getTotalElements());
            resultMap.put("pages", p.getTotalPages());
            resultMap.put("current", page);
            resultMap.put("size", size);
            // 添加统计信息，方便前端了解有多少双方都删除的消息
            resultMap.put("bothDeletedCount", bothDeletedCount);
            return Result.ok(resultMap);
        } catch (Exception e) {
            log.error("获取消息列表异常: conversationId={}, error={}", conversationId, e.getMessage(), e);
            return Result.fail("获取消息列表失败: " + e.getMessage());
        }
    }

    // ===================== 简化映射，避免循环引用 =====================
    /**
     * 将管理员实体转换为简单Map（不包含密码）
     */
    private Map<String, Object> toSimpleAdmin(Admin admin) {
        Map<String, Object> m = new HashMap<>();
        m.put("adminId", admin.getAdminId());
        m.put("username", admin.getUsername());
        m.put("realName", admin.getRealName());
        m.put("email", admin.getEmail());
        m.put("department", admin.getDepartment());
        m.put("position", admin.getPosition());
        m.put("adminLevel", admin.getAdminLevel());
        m.put("permissions", admin.getPermissions());
        m.put("accountStatus", admin.getAccountStatus());
        m.put("createTime", admin.getCreateTime());
        m.put("updateTime", admin.getUpdateTime());
        m.put("lastLoginTime", admin.getLastLoginTime());
        m.put("lastLoginIp", admin.getLastLoginIp());
        m.put("loginCount", admin.getLoginCount());
        m.put("remark", admin.getRemark());
        // ✅ 不包含密码字段
        return m;
    }

    /**
     * 将UserInternalDTO转换为User实体
     */
    private User convertUserDTOToEntity(UserInternalDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setUserId(dto.getUserId());
        user.setUsername(dto.getUsername());
        user.setPrimaryPhone(dto.getPrimaryPhone());
        user.setAccountStatus(dto.getAccountStatus());
        user.setRegisterTime(dto.getRegisterTime());
        // 其他字段使用默认值或null
        return user;
    }
    
    private Map<String, Object> toSimpleUser(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", u.getUserId());
        m.put("primaryPhone", u.getPrimaryPhone());
        m.put("username", u.getUsername());
        m.put("accountStatus", u.getAccountStatus());
        m.put("registerTime", u.getRegisterTime());
        if (u.getUserProfile() != null) {
            Map<String, Object> p = new HashMap<>();
            p.put("nickname", u.getUserProfile().getNickname());
            p.put("avatar", u.getUserProfile().getAvatar());
            p.put("creditScore", u.getUserProfile().getCreditScore());
            p.put("buyerRating", u.getUserProfile().getBuyerRating());
            p.put("sellerRating", u.getUserProfile().getSellerRating());
            p.put("totalSales", u.getUserProfile().getTotalSales());
            p.put("totalPurchases", u.getUserProfile().getTotalPurchases());
            p.put("vipLevel", u.getUserProfile().getVipLevel());
            m.put("profile", p);
        }
        return m;
    }

    private Map<String, Object> toSimpleCommodity(com.njumarket.njumarket.entity.Commodity c) {
        return toSimpleCommodityWithSeller(c, null);
    }
    
    /**
     * 将商品实体转换为简单Map（包含卖家信息）
     * @param c 商品实体
     * @param sellerProfile 卖家Profile（可选，如果为null则不在结果中包含卖家信息）
     * @return 简单Map对象
     */
    private Map<String, Object> toSimpleCommodityWithSeller(com.njumarket.njumarket.entity.Commodity c, UserProfileInternalDTO sellerProfile) {
        Map<String, Object> m = new HashMap<>();
        m.put("commodityId", c.getCommodityId());
        m.put("sellerId", c.getSellerId());
        m.put("title", c.getTitle());
        m.put("price", c.getPrice());
        m.put("stock", c.getStock());
        m.put("location", c.getLocation());
        m.put("commodityStatus", c.getCommodityStatus());
        m.put("publishTime", c.getPublishTime());
        m.put("category", c.getCategory());
        m.put("conditionLevel", c.getConditionLevel());
        m.put("description", c.getDescription());
        m.put("clickCount", c.getClickCount());
        m.put("reportCount", c.getReportCount());
        m.put("sellerVisibility", c.getSellerVisibility());
        m.put("buyerVisibility", c.getBuyerVisibility());
        m.put("images", c.getImages());
        
        // ✅ 添加卖家信息（如果提供了Profile）
        if (sellerProfile != null) {
            Map<String, Object> sellerInfo = new HashMap<>();
            sellerInfo.put("userId", c.getSellerId());
            sellerInfo.put("nickname", sellerProfile.getNickname());
            sellerInfo.put("avatar", sellerProfile.getAvatar());
            m.put("seller", sellerInfo);
        }
        
        return m;
    }

    @Override
    public Result updateCommodityFull(String commodityId, Map<String, Object> payload) {
        try {
            return commodityClient.updateCommodityFull(commodityId, payload);
        } catch (Exception e) {
            log.error("完整更新商品异常: commodityId={}, error={}", commodityId, e.getMessage(), e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    private Map<String, Object> toSimpleOrder(com.njumarket.njumarket.entity.Order o) {
        return toSimpleOrderWithUsers(o, null, null);
    }
    
    /**
     * 将订单实体转换为简单Map（包含买家和卖家信息）
     * @param o 订单实体
     * @param buyerProfile 买家Profile（可选，如果为null则不在结果中包含买家信息）
     * @param sellerProfile 卖家Profile（可选，如果为null则不在结果中包含卖家信息）
     * @return 简单Map对象
     */
    private Map<String, Object> toSimpleOrderWithUsers(com.njumarket.njumarket.entity.Order o, 
                                                       UserProfileInternalDTO buyerProfile,
                                                       UserProfileInternalDTO sellerProfile) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("buyerId", o.getBuyerId());
        m.put("sellerId", o.getSellerId());
        m.put("commodityId", o.getCommodityId());
        m.put("orderStatus", o.getOrderStatus());
        m.put("payAmount", o.getPayAmount());
        m.put("createTime", o.getCreateTime());
        m.put("payTime", o.getPayTime());
        m.put("shippingTime", o.getShippingTime());
        m.put("deliveryTime", o.getDeliveryTime());
        m.put("trackingNumber", o.getTrackingNumber());
        // 可见性与地址/备注
        m.put("sellerVisibility", o.getSellerVisibility());
        m.put("buyerVisibility", o.getBuyerVisibility());
        m.put("shippingAddress", o.getShippingAddress());
        m.put("remark", o.getRemark());
        // 退货/退款相关
        m.put("returnReason", o.getReturnReason());
        m.put("returnRequestTime", o.getReturnRequestTime());
        m.put("returnApprovalTime", o.getReturnApprovalTime());
        m.put("returnRejectionReason", o.getReturnRejectionReason());
        m.put("returnTrackingNumber", o.getReturnTrackingNumber());
        m.put("returnCompletionTime", o.getReturnCompletionTime());
        // 数量
        m.put("quantity", o.getQuantity());
        // 商品快照
        m.put("commoditySnapshotTitle", o.getCommoditySnapshotTitle());
        m.put("commoditySnapshotDescription", o.getCommoditySnapshotDescription());
        m.put("commoditySnapshotPrice", o.getCommoditySnapshotPrice());
        m.put("commoditySnapshotLocation", o.getCommoditySnapshotLocation());
        m.put("commoditySnapshotCategory", o.getCommoditySnapshotCategory());
        m.put("commoditySnapshotConditionLevel", o.getCommoditySnapshotConditionLevel());
        m.put("commoditySnapshotImages", o.getCommoditySnapshotImages());
        m.put("commoditySnapshotStatus", o.getCommoditySnapshotStatus());
        m.put("commoditySnapshotSellerName", o.getCommoditySnapshotSellerName());
        m.put("commoditySnapshotSellerPhone", o.getCommoditySnapshotSellerPhone());
        m.put("commoditySnapshotSellerEmail", o.getCommoditySnapshotSellerEmail());
        m.put("commoditySnapshotTime", o.getCommoditySnapshotTime());
        
        // ✅ 添加买家信息（如果提供了Profile）
        if (buyerProfile != null) {
            Map<String, Object> buyerInfo = new HashMap<>();
            buyerInfo.put("userId", o.getBuyerId());
            buyerInfo.put("nickname", buyerProfile.getNickname());
            buyerInfo.put("avatar", buyerProfile.getAvatar());
            m.put("buyer", buyerInfo);
        }
        
        // ✅ 添加卖家信息（如果提供了Profile）
        if (sellerProfile != null) {
            Map<String, Object> sellerInfo = new HashMap<>();
            sellerInfo.put("userId", o.getSellerId());
            sellerInfo.put("nickname", sellerProfile.getNickname());
            sellerInfo.put("avatar", sellerProfile.getAvatar());
            m.put("seller", sellerInfo);
        }
        
        return m;
    }

    private Map<String, Object> toSimpleConversation(Conversation c) {
        return toSimpleConversationWithUsers(c, null, null);
    }
    
    /**
     * 将会话实体转换为简单Map（包含用户信息）
     * @param c 会话实体
     * @param user1Profile 用户1的Profile（可选，如果为null则不在结果中包含用户信息）
     * @param user2Profile 用户2的Profile（可选，如果为null则不在结果中包含用户信息）
     * @return 简单Map对象
     */
    private Map<String, Object> toSimpleConversationWithUsers(Conversation c,
                                                               UserProfileInternalDTO user1Profile,
                                                               UserProfileInternalDTO user2Profile) {
        Map<String, Object> m = new HashMap<>();
        m.put("conversationId", c.getConversationId());
        m.put("userId1", c.getUserId1());
        m.put("userId2", c.getUserId2());
        m.put("user1Count", c.getUser1Count());
        m.put("user2Count", c.getUser2Count());
        
        // ✅ 管理端字段（不过滤，显示真实最新消息，包括双方都删除的）
        m.put("lastMessageContent", c.getLastMessageContent());
        m.put("lastMessageTime", c.getLastMessageTime());
        
        // ✅ 用户级别的最后消息字段（过滤用户删除的）
        m.put("user1LastMessageContent", c.getUser1LastMessageContent());
        m.put("user1LastMessageTime", c.getUser1LastMessageTime());
        m.put("user2LastMessageContent", c.getUser2LastMessageContent());
        m.put("user2LastMessageTime", c.getUser2LastMessageTime());
        
        m.put("status", c.getStatus());
        m.put("createdAt", c.getCreatedAt());
        m.put("updatedAt", c.getUpdatedAt());
        
        // ✅ 添加可见性字段（管理端可以看到所有会话，不受可见性限制，但仍显示字段值）
        m.put("user1Visibility", c.getUser1Visibility() != null ? c.getUser1Visibility() : true);
        m.put("user2Visibility", c.getUser2Visibility() != null ? c.getUser2Visibility() : true);
        
        // ✅ 添加用户1信息（如果提供了Profile）
        if (user1Profile != null) {
            Map<String, Object> user1Info = new HashMap<>();
            user1Info.put("userId", c.getUserId1());
            user1Info.put("nickname", user1Profile.getNickname());
            user1Info.put("avatar", user1Profile.getAvatar());
            m.put("user1", user1Info);
        }
        
        // ✅ 添加用户2信息（如果提供了Profile）
        if (user2Profile != null) {
            Map<String, Object> user2Info = new HashMap<>();
            user2Info.put("userId", c.getUserId2());
            user2Info.put("nickname", user2Profile.getNickname());
            user2Info.put("avatar", user2Profile.getAvatar());
            m.put("user2", user2Info);
        }
        
        return m;
    }

    private Map<String, Object> toSimpleMessage(Message m0) {
        Map<String, Object> m = new HashMap<>();
        m.put("messageId", m0.getMessageId());
        m.put("conversationId", m0.getConversationId());
        m.put("senderId", m0.getSenderId());
        m.put("receiverId", m0.getReceiverId());
        m.put("messageType", m0.getMessageType() != null ? m0.getMessageType() : "TEXT");
        m.put("content", m0.getContent());
        m.put("imageUrl", m0.getImageUrl());
        m.put("commodityId", m0.getCommodityId());
        m.put("orderId", m0.getOrderId());
        m.put("isRead", m0.getIsRead());
        m.put("deletedBySender", m0.getDeletedBySender() != null ? m0.getDeletedBySender() : false);
        m.put("deletedByReceiver", m0.getDeletedByReceiver() != null ? m0.getDeletedByReceiver() : false);
        m.put("createdAt", m0.getCreatedAt());
        m.put("readTime", m0.getReadTime());
        return m;
    }

    @Override
    public Result getMessageById(String messageId) {
        try {
            Result result = messageClient.getMessageById(messageId);
            if (!result.getSuccess() || result.getData() == null) {
                return Result.fail(result.getErrorMsg() != null ? result.getErrorMsg() : "消息不存在");
            }
            // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
            Message message;
            try {
                message = objectMapper.convertValue(
                    result.getData(),
                    new TypeReference<Message>() {}
                );
            } catch (Exception e) {
                log.error("转换Message失败: messageId={}, error={}", messageId, e.getMessage(), e);
                return Result.fail("消息信息解析失败");
            }
            return Result.ok(toSimpleMessage(message));
        } catch (Exception e) {
            log.error("获取消息详情异常: messageId={}, error={}", messageId, e.getMessage(), e);
            return Result.fail("获取消息详情失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateMessageFull(String messageId, java.util.Map<String, Object> payload) {
        try {
            // 调用message-service更新消息（复杂逻辑由message-service处理）
            Result updateResult = messageClient.updateMessageFull(messageId, payload);
            if (!updateResult.getSuccess()) {
                return updateResult;
            }
            
            // 重新获取消息以返回完整数据
            Result getUpdatedResult = messageClient.getMessageById(messageId);
            if (!getUpdatedResult.getSuccess() || getUpdatedResult.getData() == null) {
                return Result.ok("消息更新成功");
            }
            // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
            Message updatedMessage;
            try {
                updatedMessage = objectMapper.convertValue(
                    getUpdatedResult.getData(),
                    new TypeReference<Message>() {}
                );
            } catch (Exception e) {
                log.error("转换Message失败: messageId={}, error={}", messageId, e.getMessage(), e);
                return Result.ok("消息更新成功");
            }
            return Result.ok("消息更新成功", toSimpleMessage(updatedMessage));
        } catch (Exception e) {
            log.error("完整更新消息异常: messageId={}, error={}", messageId, e.getMessage());
            return Result.fail("更新消息失败");
        }
    }

    @Override
    public Result deleteMessage(String messageId) {
        try {
            return messageClient.deleteMessage(messageId);
        } catch (Exception e) {
            log.error("删除消息异常: messageId={}, error={}", messageId, e.getMessage(), e);
            return Result.fail("删除消息失败: " + e.getMessage());
        }
    }
}

