package com.njumarket.admin.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.AdminLoginDTO;
import com.njumarket.njumarket.vo.*;
import com.njumarket.njumarket.entity.Admin;
import com.njumarket.njumarket.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.njumarket.entity.Conversation;
import com.njumarket.njumarket.entity.Message;
import com.njumarket.admin.repository.AdminRepository;
import com.njumarket.admin.repository.UserRepository;
import com.njumarket.admin.repository.UserProfileRepository;
import com.njumarket.admin.repository.CommodityRepository;
import com.njumarket.admin.repository.OrderRepository;
import com.njumarket.admin.repository.ConversationRepository;
import com.njumarket.admin.repository.MessageRepository;
import com.njumarket.njumarket.entity.UserProfile;
import com.njumarket.admin.service.AdminService;
import com.njumarket.admin.service.PasswordService;
import com.njumarket.njumarket.utils.JwtUtils;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.entity.Order;
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
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CommodityRepository commodityRepository;
    private final OrderRepository orderRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PasswordService passwordService;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Override
    public Result login(AdminLoginDTO loginDTO, HttpSession session) {
        try {
            // 参数验证已由@Valid注解自动完成，无需手动验证

            // 1. 查找管理员
            Optional<Admin> adminOpt = adminRepository.findByUsername(loginDTO.getUsername().trim());
            if (adminOpt.isEmpty()) {
                log.warn("error in username, 管理员登录失败: 用户名不存在, username={}", loginDTO.getUsername());
                throw new BusinessException("用户名或密码错误");
            }

            Admin admin = adminOpt.get();

            // 3. 检查账户状态
            if (!admin.canLogin()) {
                log.warn("管理员登录失败: 账户被禁用, username={}", loginDTO.getUsername());
                throw new BusinessException("账户已被禁用，请联系系统管理员");
            }

            // 4. 验证密码
            if (!passwordService.matches(loginDTO.getPassword(), admin.getPassword())) {
                log.warn("error in password, 管理员登录失败: 密码错误, username={}", loginDTO.getUsername());
                throw new BusinessException("用户名或密码错误");
            }

            // 5. 更新登录信息
            String clientIp = "127.0.0.1"; // 暂时使用默认IP，后续可以从request中获取
            admin.updateLoginInfo(clientIp);
            adminRepository.save(admin);

            // 6. 生成Token
            AdminLoginResultVO tokenResult = generateAndStoreTokens(admin);

            // 7. 存储到Session
            session.setAttribute("admin", admin);
            session.setAttribute("adminId", admin.getAdminId());

            log.info("管理员登录成功: adminId={}, username={}, ip={}", 
                admin.getAdminId(), admin.getUsername(), clientIp);

            return Result.ok(tokenResult);

        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("管理员登录异常: username={}, error={}", loginDTO.getUsername(), e.getMessage());
            throw new BusinessException("登录失败，请稍后重试");
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
        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("管理员登出异常: {}", e.getMessage());
            throw new BusinessException("登出失败");
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
        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("获取当前管理员信息异常: {}", e.getMessage());
            throw new BusinessException("获取管理员信息失败");
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
                throw new BusinessException("权限不足，只有system权限的管理员才能创建管理员");
            }

            // 1. 参数验证
            if (!StringUtils.hasText(admin.getUsername())) {
                throw new BusinessException("用户名不能为空");
            }
            if (!StringUtils.hasText(admin.getPassword())) {
                throw new BusinessException("密码不能为空");
            }
            if (admin.getPassword().length() < 6) {
                throw new BusinessException("密码长度不能少于6位");
            }

            // 2. 检查用户名是否已存在
            if (adminRepository.existsByUsername(admin.getUsername())) {
                throw new BusinessException("用户名已存在");
            }

            // ✅ 3. 限制：新创建的管理员级别只能是administrator，不能创建system管理员
            if (admin.getAdminLevel() != null && "system".equals(admin.getAdminLevel())) {
                throw new BusinessException("不允许创建system权限的管理员");
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

            AdminSimpleVO result = toSimpleAdmin(savedAdmin);
            return Result.ok("创建管理员成功", result);

        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("创建管理员异常: username={}, error={}", admin.getUsername(), e.getMessage(), e);
            throw new BusinessException("创建管理员失败：" + e.getMessage());
        }
    }

    @Override
    public Result updateAdmin(String adminId, Admin admin) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                throw new BusinessException("管理员不存在");
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

        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("更新管理员异常: adminId={}, error={}", adminId, e.getMessage());
            throw new BusinessException("更新管理员失败");
        }
    }

    @Override
    public Result deleteAdmin(String adminId) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                throw new BusinessException("管理员不存在");
            }

            Admin admin = adminOpt.get();
            
            // 检查是否为系统管理员
            if (admin.isSystemAdmin()) {
                throw new BusinessException("不能删除系统管理员");
            }

            adminRepository.deleteById(adminId);

            log.info("删除管理员成功: adminId={}, username={}", admin.getAdminId(), admin.getUsername());

            return Result.ok("删除成功");

        } catch (Exception e) {
            log.error("删除管理员异常: adminId={}, error={}", adminId, e.getMessage());
            throw new BusinessException("删除管理员失败");
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
                throw new BusinessException("权限不足，只有system权限的管理员才能查看管理员列表");
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
            List<AdminSimpleVO> adminList = adminPage.getContent().stream()
                    .map(this::toSimpleAdmin)
                    .collect(java.util.stream.Collectors.toList());

            PageResultVO<AdminSimpleVO> result = new PageResultVO<>();
            result.setList(adminList);
            result.setTotal(adminPage.getTotalElements());
            result.setCurrent(page);
            result.setSize(size);
            result.setPages(adminPage.getTotalPages());

            return Result.ok(result);

        } catch (Exception e) {
            log.error("获取管理员列表异常: error={}", e.getMessage(), e);
            throw new BusinessException("获取管理员列表失败");
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
                throw new BusinessException("权限不足，只有system权限的管理员才能查看管理员信息");
            }

            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                throw new BusinessException("管理员不存在");
            }

            Admin admin = adminOpt.get();
            // ✅ 返回简化的管理员信息（不包含密码）
            AdminSimpleVO adminMap = toSimpleAdmin(admin);

            return Result.ok(adminMap);

        } catch (Exception e) {
            log.error("获取管理员信息异常: adminId={}, error={}", adminId, e.getMessage());
            throw new BusinessException("获取管理员信息失败");
        }
    }

    @Override
    public Result updateAdminStatus(String adminId, String status) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                throw new BusinessException("管理员不存在");
            }

            Admin admin = adminOpt.get();
            admin.setAccountStatus(status);
            admin.setUpdateTime(LocalDateTime.now());

            adminRepository.save(admin);

            log.info("更新管理员状态成功: adminId={}, status={}", adminId, status);

            return Result.ok("状态更新成功");

        } catch (Exception e) {
            log.error("更新管理员状态异常: adminId={}, status={}, error={}", adminId, status, e.getMessage());
            throw new BusinessException("更新状态失败");
        }
    }

    @Override
    public Result resetPassword(String adminId, String newPassword) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                throw new BusinessException("管理员不存在");
            }

            Admin admin = adminOpt.get();
            admin.setPassword(passwordService.encodePassword(newPassword));
            admin.setUpdateTime(LocalDateTime.now());

            adminRepository.save(admin);

            log.info("重置管理员密码成功: adminId={}", adminId);

            return Result.ok("密码重置成功");

        } catch (Exception e) {
            log.error("重置管理员密码异常: adminId={}, error={}", adminId, e.getMessage());
            throw new BusinessException("密码重置失败");
        }
    }

    @Override
    public Result changePassword(String adminId, String oldPassword, String newPassword) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                throw new BusinessException("管理员不存在");
            }

            Admin admin = adminOpt.get();

            // 验证旧密码
            if (!passwordService.matches(oldPassword, admin.getPassword())) {
                throw new BusinessException("原密码错误");
            }

            // 更新密码
            admin.setPassword(passwordService.encodePassword(newPassword));
            admin.setUpdateTime(LocalDateTime.now());

            adminRepository.save(admin);

            log.info("修改管理员密码成功: adminId={}", adminId);

            return Result.ok("密码修改成功");

        } catch (Exception e) {
            log.error("修改管理员密码异常: adminId={}, error={}", adminId, e.getMessage());
            throw new BusinessException("密码修改失败");
        }
    }

    @Override
    public Result getAdminStatistics() {
        try {
            AdminStatisticsVO statistics = new AdminStatisticsVO();
            
            // 总管理员数
            long totalAdmins = adminRepository.count();
            statistics.setTotalAdmins(totalAdmins);
            
            // 活跃管理员数
            long activeAdmins = adminRepository.countActiveAdmins();
            statistics.setActiveAdmins(activeAdmins);
            
            // 各级别管理员数量
            List<Object[]> levelCounts = adminRepository.countAdminsByLevel();
            Map<String, Long> levelStats = new HashMap<>();
            for (Object[] levelCount : levelCounts) {
                levelStats.put((String) levelCount[0], (Long) levelCount[1]);
            }
            statistics.setLevelStats(levelStats);
            
            // 系统管理员数量
            long systemAdmins = adminRepository.findByAdminLevel("system").size();
            statistics.setSystemAdmins(systemAdmins);
            
            // 普通管理员数量
            long administratorCount = adminRepository.findByAdminLevel("administrator").size();
            statistics.setAdministratorCount(administratorCount);
            
            return Result.ok(statistics);

        } catch (Exception e) {
            log.error("获取管理员统计信息异常: error={}", e.getMessage());
            throw new BusinessException("获取统计信息失败");
        }
    }

    @Override
    public Result checkPermission(String adminId, String permission) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                throw new BusinessException("管理员不存在");
            }

            Admin admin = adminOpt.get();
            boolean hasPermission = admin.hasPermission(permission);

            PermissionCheckVO result = new PermissionCheckVO();
            result.setAdminId(adminId);
            result.setPermission(permission);
            result.setHasPermission(hasPermission);

            return Result.ok(result);

        } catch (Exception e) {
            log.error("检查管理员权限异常: adminId={}, permission={}, error={}", adminId, permission, e.getMessage());
            throw new BusinessException("权限检查失败");
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
                throw new BusinessException("权限不足，只有system权限的管理员才能更新管理员信息");
            }

            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                throw new BusinessException("管理员不存在");
            }

            Admin admin = adminOpt.get();

            // ✅ 更新所有非客观字段（不包括createTime、updateTime、lastLoginTime、loginCount等）
            if (payload.containsKey("username")) {
                String username = String.valueOf(payload.get("username")).trim();
                if (username.isEmpty()) {
                    throw new BusinessException("用户名不能为空");
                }
                // 检查用户名是否已被其他管理员使用
                Optional<Admin> existingAdmin = adminRepository.findByUsername(username);
                if (existingAdmin.isPresent() && !existingAdmin.get().getAdminId().equals(adminId)) {
                    throw new BusinessException("用户名已被使用");
                }
                admin.setUsername(username);
            }

            if (payload.containsKey("password")) {
                // ✅ 更新密码
                String newPassword = String.valueOf(payload.get("password")).trim();
                if (newPassword.isEmpty()) {
                    throw new BusinessException("密码不能为空");
                }
                if (newPassword.length() < 6) {
                    throw new BusinessException("密码长度不能少于6位");
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
                throw new BusinessException("管理员级别为固定字段，不允许修改");
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
                    throw new BusinessException("无效的账户状态");
                }
                
                // ✅ 禁止修改系统管理员的账户状态
                if ("system".equals(admin.getAdminLevel())) {
                    throw new BusinessException("系统管理员的账户状态不允许修改");
                }
                
                admin.setAccountStatus(status);
            }

            if (payload.containsKey("remark")) {
                admin.setRemark(payload.get("remark") != null ? String.valueOf(payload.get("remark")).trim() : null);
            }

            // updateTime 由 @UpdateTimestamp 自动更新，不需要手动设置
            adminRepository.save(admin);

            log.info("更新管理员信息成功: adminId={}, operatorId={}", adminId, currentAdmin.getAdminId());

            AdminSimpleVO result = toSimpleAdmin(admin);
            return Result.ok("更新成功", result);

        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("更新管理员信息异常: adminId={}, error={}", adminId, e.getMessage(), e);
            throw new BusinessException("更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result updatePermissions(String adminId, List<String> permissions) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                throw new BusinessException("管理员不存在");
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
            throw new BusinessException("权限更新失败");
        }
    }

    /**
     * 生成并存储Token
     */
    private AdminLoginResultVO generateAndStoreTokens(Admin admin) {
        // 生成JWT Token
        String token = jwtUtils.generateToken(admin.getAdminId(), admin.getUsername());
        
        AdminLoginResultVO result = new AdminLoginResultVO();
        result.setToken(token);
        result.setAdminId(admin.getAdminId());
        result.setUsername(admin.getUsername());
        result.setAdminLevel(admin.getAdminLevel());
        result.setExpiresIn(24 * 60 * 60L); // 24小时
        
        return result;
    }

    // ===================== 管理端最小CRUD：用户 =====================
    @Override
    public Result listUsers(Integer page, Integer size, String keyword, String accountStatus, String sortProp, String sortOrder) {
        try {
            // ✅ 直接访问数据库，不需要通过FeignClient
            // 构建分页参数
            Sort sort = Sort.by(Sort.Direction.DESC, "registerTime");
            if (StringUtils.hasText(sortProp)) {
                Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
                sort = Sort.by(direction, sortProp);
            }
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);
            
            // 构建查询条件
            org.springframework.data.jpa.domain.Specification<User> spec = (root, query, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                
                // 关键词搜索：用户名、手机号、用户ID（处理空字符串）
                if (keyword != null && !keyword.trim().isEmpty()) {
                    String kw = keyword.trim().toLowerCase();
                    predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("primaryPhone")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("userId")), "%" + kw + "%")
                    ));
                }
                
                // 账户状态筛选（处理空字符串）
                if (accountStatus != null && !accountStatus.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("accountStatus"), accountStatus.trim()));
                }
                
                return predicates.isEmpty() ? cb.conjunction() : 
                    cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            
            Page<User> userPage = userRepository.findAll(spec, pageable);
            
            // ✅ 批量查询所有用户的 UserProfile（避免 N+1 查询）
            List<User> users = userPage.getContent();
            Set<String> userIds = users.stream()
                    .map(User::getUserId)
                    .collect(java.util.stream.Collectors.toSet());
            
            Map<String, UserProfile> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
                profileMap = profiles.stream()
                        .collect(java.util.stream.Collectors.toMap(UserProfile::getUserId, profile -> profile));
            }
            
            // ✅ 设置Profile到User实体（与旧项目保持一致，toSimpleUser方法会直接使用u.getUserProfile()）
            final Map<String, UserProfile> finalProfileMap = profileMap;
            for (User user : users) {
                UserProfile profile = finalProfileMap.get(user.getUserId());
                if (profile != null) {
                    user.setUserProfile(profile);
                }
            }
            
            // ✅ 转换为包含用户信息的简单对象（与旧项目保持一致）
            List<Map<String, Object>> simpleList = users.stream()
                    .map(this::toSimpleUser)
                    .collect(java.util.stream.Collectors.toList());
            
            PageResultVO<Map<String, Object>> result = new PageResultVO<>();
            result.setList(simpleList);
            result.setTotal(userPage.getTotalElements());
            result.setPages(userPage.getTotalPages());
            result.setCurrent(page);
            result.setSize(size);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取用户列表异常: {}", e.getMessage(), e);
            throw new BusinessException("获取用户列表失败");
        }
    }

    @Override
    public Result getUserById(String userId) {
        try {
            Optional<User> opt = userRepository.findById(userId);
            if (opt.isEmpty()) {
                throw new BusinessException("用户不存在");
            }
            User user = opt.get();
            
            // ✅ 手动查询Profile（因为User.userProfile是LAZY加载）
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isPresent()) {
                user.setUserProfile(profileOpt.get());
            }
            
            return Result.ok(toSimpleUser(user));
        } catch (Exception e) {
            log.error("获取用户信息异常: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException("获取用户信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateUserStatus(String userId, String status) {
        try {
            Optional<User> opt = userRepository.findById(userId);
            if (opt.isEmpty()) {
                throw new BusinessException("用户不存在");
            }
            User user = opt.get();
            user.setAccountStatus(status);
            userRepository.save(user);
            
            // ✅ 设置Profile到User实体（确保toSimpleUser能正确获取Profile）
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isPresent()) {
                user.setUserProfile(profileOpt.get());
            }
            
            return Result.ok("更新成功", toSimpleUser(user));
        } catch (Exception e) {
            log.error("更新用户状态异常: userId={}, status={}, error={}", userId, status, e.getMessage(), e);
            throw new BusinessException("更新用户状态失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateUserBasic(String userId, String nickname, String phone, String email) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new BusinessException("用户不存在");
            }
            User user = userOpt.get();
            
            // 更新用户基本信息
            if (phone != null) user.setPrimaryPhone(phone);
            
            // 更新UserProfile
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            UserProfile profile = null;
            if (profileOpt.isPresent()) {
                profile = profileOpt.get();
                if (nickname != null) profile.setNickname(nickname);
                // email通常存储在UserProfile中，但User实体中没有email字段，这里暂时跳过
                userProfileRepository.save(profile);
            }
            
            userRepository.save(user);
            
            // ✅ 设置Profile到User实体（确保toSimpleUser能正确获取Profile）
            if (profile != null) {
                user.setUserProfile(profile);
            }
            
            return Result.ok("更新成功", toSimpleUser(user));
        } catch (Exception e) {
            log.error("更新用户基础信息异常: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException("更新用户信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result deleteUser(String userId) {
        try {
            if (!userRepository.existsById(userId)) {
                throw new BusinessException("用户不存在");
            }
            userRepository.deleteById(userId);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除用户异常: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException("删除用户失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateUserFull(String userId, Map<String, Object> payload) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new BusinessException("用户不存在");
            }
            User user = userOpt.get();
            
            // 基本字段
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
                if (!allowed.contains(newStatus)) {
                    throw new BusinessException("非法的账户状态");
                }
                user.setAccountStatus(newStatus);
            }

            // 档案字段
            UserProfile profile = user.getUserProfile();
            if (profile == null) {
                profile = new UserProfile();
                profile.setProfileId("PROFILE_" + System.currentTimeMillis());
                profile.setUserId(user.getUserId());
            }
            
            Object nickname = payload.get("nickname");
            if (nickname instanceof String) {
                profile.setNickname(((String) nickname).trim());
            }
            
            Object avatar = payload.get("avatar");
            if (avatar instanceof String) {
                profile.setAvatar(((String) avatar).trim());
            }
            
            // ✅ 信用分：支持 Number 和 String 类型
            Object creditScore = payload.get("creditScore");
            if (creditScore != null) {
                try {
                    int score = creditScore instanceof Number 
                        ? ((Number) creditScore).intValue() 
                        : Integer.parseInt(creditScore.toString().trim());
                    if (score >= 0 && score <= 100) {
                        profile.setCreditScore(score);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            // ✅ 买家评分：支持 Number 和 String 类型
            Object buyerRating = payload.get("buyerRating");
            if (buyerRating != null) {
                try {
                    double rating = buyerRating instanceof Number 
                        ? ((Number) buyerRating).doubleValue() 
                        : Double.parseDouble(buyerRating.toString().trim());
                    if (rating >= 0 && rating <= 5) {
                        profile.setBuyerRating(rating);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            // ✅ 卖家评分：支持 Number 和 String 类型
            Object sellerRating = payload.get("sellerRating");
            if (sellerRating != null) {
                try {
                    double rating = sellerRating instanceof Number 
                        ? ((Number) sellerRating).doubleValue() 
                        : Double.parseDouble(sellerRating.toString().trim());
                    if (rating >= 0 && rating <= 5) {
                        profile.setSellerRating(rating);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            // ✅ 卖出次数：支持 Number 和 String 类型
            Object totalSales = payload.get("totalSales");
            if (totalSales != null) {
                try {
                    int sales = totalSales instanceof Number 
                        ? ((Number) totalSales).intValue() 
                        : Integer.parseInt(totalSales.toString().trim());
                    if (sales >= 0) {
                        profile.setTotalSales(sales);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            // ✅ 购入次数：支持 Number 和 String 类型
            Object totalPurchases = payload.get("totalPurchases");
            if (totalPurchases != null) {
                try {
                    int purchases = totalPurchases instanceof Number 
                        ? ((Number) totalPurchases).intValue() 
                        : Integer.parseInt(totalPurchases.toString().trim());
                    if (purchases >= 0) {
                        profile.setTotalPurchases(purchases);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            Object vipLevel = payload.get("vipLevel");
            if (vipLevel instanceof String && StringUtils.hasText((String) vipLevel)) {
                String lvl = ((String) vipLevel).trim();
                java.util.Set<String> allowedLvl = new java.util.HashSet<>(java.util.Arrays.asList("NORMAL","BRONZE","SILVER","GOLD","PLATINUM"));
                if (!allowedLvl.contains(lvl)) {
                    throw new BusinessException("非法的会员等级");
                }
                profile.setVipLevel(lvl);
            }

            userRepository.save(user);
            userProfileRepository.save(profile);

            return Result.ok("更新成功", toSimpleUser(user));
        } catch (Exception e) {
            log.error("完整更新用户异常: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException("更新失败: " + e.getMessage());
        }
    }

    // ===================== 管理端最小CRUD：商品 =====================
    @Override
    public Result listCommodities(Integer page, Integer size, String keyword, String category, String conditionLevel, String status, String sellerVisibility, String buyerVisibility, String sortProp, String sortOrder) {
        try {
            // ✅ 直接访问数据库，不需要通过FeignClient
            // 排序（默认 publishTime desc）
            Sort sort = Sort.by(Sort.Direction.DESC, "publishTime");
            if (StringUtils.hasText(sortProp)) {
                String sp = sortProp.trim();
                Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
                // ✅ 支持publishTime、clickCount、price排序
                if ("publishTime".equals(sp) || "clickCount".equals(sp) || "price".equals(sp)) {
                    sort = Sort.by(direction, sp);
                }
            }
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);
            
            // 构建查询条件
            org.springframework.data.jpa.domain.Specification<Commodity> spec = (root, query, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                
                // 关键词搜索：标题、描述、商品ID、卖家ID、卖家昵称（处理空字符串）
                if (keyword != null && !keyword.trim().isEmpty()) {
                    String kw = keyword.trim().toLowerCase();
                    jakarta.persistence.criteria.Expression<String> title = cb.lower(root.get("title"));
                    jakarta.persistence.criteria.Expression<String> description = cb.lower(root.get("description"));
                    jakarta.persistence.criteria.Expression<String> commodityId = cb.lower(root.get("commodityId"));
                    jakarta.persistence.criteria.Expression<String> sellerIdExp = cb.lower(root.get("sellerId"));
                    
                    // 子查询：按卖家昵称（UserProfile.nickname）模糊匹配
                    jakarta.persistence.criteria.Subquery<User> sq = query.subquery(User.class);
                    jakarta.persistence.criteria.Root<User> ur = sq.from(User.class);
                    // LEFT JOIN userProfile
                    jakarta.persistence.criteria.Join<User, UserProfile> profileJoin = ur.join("userProfile", jakarta.persistence.criteria.JoinType.LEFT);
                    jakarta.persistence.criteria.Predicate sellerMatch = cb.equal(ur.get("userId"), root.get("sellerId"));
                    jakarta.persistence.criteria.Predicate nickLike = cb.like(cb.lower(profileJoin.get("nickname")), "%" + kw + "%");
                    sq.select(ur).where(cb.and(sellerMatch, nickLike));
                    
                    predicates.add(cb.or(
                        cb.like(title, "%" + kw + "%"),
                        cb.like(description, "%" + kw + "%"),
                        cb.like(commodityId, "%" + kw + "%"),
                        cb.like(sellerIdExp, "%" + kw + "%"),
                        cb.exists(sq)
                    ));
                }
                
                // 分类筛选（处理空字符串）
                if (category != null && !category.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("category"), category.trim()));
                }
                
                // 成色筛选（处理空字符串）
                if (conditionLevel != null && !conditionLevel.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("conditionLevel"), conditionLevel.trim()));
                }
                
                // 状态筛选（处理空字符串）
                if (status != null && !status.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("commodityStatus"), status.trim()));
                }
                
                // 卖家可见性筛选（处理空字符串）
                if (sellerVisibility != null && !sellerVisibility.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("sellerVisibility"), sellerVisibility.trim()));
                }
                
                // 买家可见性筛选（处理空字符串）
                if (buyerVisibility != null && !buyerVisibility.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("buyerVisibility"), buyerVisibility.trim()));
                }
                
                return predicates.isEmpty() ? cb.conjunction() : 
                    cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            
            Page<Commodity> commodityPage = commodityRepository.findAll(spec, pageable);
            
            // ✅ 批量查询所有卖家的 UserProfile（避免 N+1 查询）
            List<Commodity> commodities = commodityPage.getContent();
            Set<String> sellerIds = commodities.stream()
                    .map(Commodity::getSellerId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            
            Map<String, UserProfile> profileMap = new HashMap<>();
            if (!sellerIds.isEmpty()) {
                List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(sellerIds));
                profileMap = profiles.stream()
                        .collect(java.util.stream.Collectors.toMap(UserProfile::getUserId, profile -> profile));
            }
            
            // ✅ 转换为包含卖家信息的简单对象
            final Map<String, UserProfile> finalProfileMap = profileMap;
            List<Map<String, Object>> commodityList = commodities.stream()
                    .map(c -> {
                        UserProfile sellerProfile = finalProfileMap.get(c.getSellerId());
                        UserProfileInternalDTO sellerProfileDTO = sellerProfile != null ? convertToUserProfileDTO(sellerProfile) : null;
                        return toSimpleCommodityWithSeller(c, sellerProfileDTO);
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            PageResultVO<Map<String, Object>> result = new PageResultVO<>();
            result.setList(commodityList);
            result.setTotal(commodityPage.getTotalElements());
            result.setPages(commodityPage.getTotalPages());
            result.setCurrent(page);
            result.setSize(size);
            
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取商品列表异常: error={}", e.getMessage(), e);
            throw new BusinessException("获取商品列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result getCommodityById(String commodityId) {
        try {
            Optional<Commodity> opt = commodityRepository.findById(commodityId);
            if (opt.isEmpty()) {
                throw new BusinessException("商品不存在");
            }
            Commodity commodity = opt.get();
            
            // ✅ 查询卖家Profile（避免N+1查询）
            UserProfile sellerProfile = null;
            if (commodity.getSellerId() != null) {
                Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(commodity.getSellerId());
                sellerProfile = profileOpt.orElse(null);
            }
            
            UserProfileInternalDTO sellerProfileDTO = sellerProfile != null ? convertToUserProfileDTO(sellerProfile) : null;
            return Result.ok(toSimpleCommodityWithSeller(commodity, sellerProfileDTO));
        } catch (Exception e) {
            log.error("获取商品信息异常: commodityId={}, error={}", commodityId, e.getMessage(), e);
            throw new BusinessException("获取商品信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateCommodityStatus(String commodityId, String status) {
        try {
            Optional<Commodity> opt = commodityRepository.findById(commodityId);
            if (opt.isEmpty()) {
                throw new BusinessException("商品不存在");
            }
            Commodity commodity = opt.get();
            commodity.setCommodityStatus(status);
            commodityRepository.save(commodity);
            return Result.ok("更新成功", toSimpleCommodity(commodity));
        } catch (Exception e) {
            log.error("更新商品状态异常: commodityId={}, status={}, error={}", commodityId, status, e.getMessage(), e);
            throw new BusinessException("更新商品状态失败: " + e.getMessage());
        }
    }

    @Override
    public Result deleteCommodity(String commodityId) {
        try {
            if (!commodityRepository.existsById(commodityId)) {
                throw new BusinessException("商品不存在");
            }
            commodityRepository.deleteById(commodityId);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除商品异常: commodityId={}, error={}", commodityId, e.getMessage(), e);
            throw new BusinessException("删除商品失败: " + e.getMessage());
        }
    }

    // ===================== 管理端最小CRUD：订单 =====================
    @Override
    public Result listOrders(Integer page, Integer size, String keyword, String status, String sellerVisibility, String buyerVisibility, String sortProp, String sortOrder) {
        try {
            // ✅ 直接访问数据库，不需要通过FeignClient
            // 排序（默认 createTime desc）
            Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
            if (StringUtils.hasText(sortProp)) {
                String sp = sortProp.trim();
                Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
                // ✅ 支持创建时间和金额排序
                if ("createTime".equals(sp) || "payAmount".equals(sp)) {
                    sort = Sort.by(direction, sp);
                }
            }
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);
            
            // 构建查询条件
            org.springframework.data.jpa.domain.Specification<Order> spec = (root, query, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                
                // ✅ 搜索优化：支持买家ID、卖家ID、商品标题、买家昵称、卖家昵称（处理空字符串）
                if (keyword != null && !keyword.trim().isEmpty()) {
                    String kw = keyword.trim().toLowerCase();
                    jakarta.persistence.criteria.Expression<String> buyerId = cb.lower(root.get("buyerId"));
                    jakarta.persistence.criteria.Expression<String> sellerId = cb.lower(root.get("sellerId"));
                    jakarta.persistence.criteria.Expression<String> snapTitle = cb.lower(root.get("commoditySnapshotTitle"));
                    jakarta.persistence.criteria.Expression<String> orderId = cb.lower(root.get("orderId"));
                    
                    // 子查询：按买家昵称（UserProfile.nickname）模糊匹配
                    jakarta.persistence.criteria.Subquery<User> buyerSq = query.subquery(User.class);
                    jakarta.persistence.criteria.Root<User> buyerUr = buyerSq.from(User.class);
                    jakarta.persistence.criteria.Join<User, UserProfile> buyerProfileJoin = buyerUr.join("userProfile", jakarta.persistence.criteria.JoinType.LEFT);
                    jakarta.persistence.criteria.Predicate buyerMatch = cb.equal(buyerUr.get("userId"), root.get("buyerId"));
                    jakarta.persistence.criteria.Predicate buyerNickLike = cb.like(cb.lower(buyerProfileJoin.get("nickname")), "%" + kw + "%");
                    buyerSq.select(buyerUr).where(cb.and(buyerMatch, buyerNickLike));
                    
                    // 子查询：按卖家昵称（UserProfile.nickname）模糊匹配
                    jakarta.persistence.criteria.Subquery<User> sellerSq = query.subquery(User.class);
                    jakarta.persistence.criteria.Root<User> sellerUr = sellerSq.from(User.class);
                    jakarta.persistence.criteria.Join<User, UserProfile> sellerProfileJoin = sellerUr.join("userProfile", jakarta.persistence.criteria.JoinType.LEFT);
                    jakarta.persistence.criteria.Predicate sellerMatch = cb.equal(sellerUr.get("userId"), root.get("sellerId"));
                    jakarta.persistence.criteria.Predicate sellerNickLike = cb.like(cb.lower(sellerProfileJoin.get("nickname")), "%" + kw + "%");
                    sellerSq.select(sellerUr).where(cb.and(sellerMatch, sellerNickLike));
                    
                    predicates.add(cb.or(
                        cb.like(orderId, "%" + kw + "%"),
                        cb.like(buyerId, "%" + kw + "%"),
                        cb.like(sellerId, "%" + kw + "%"),
                        cb.like(snapTitle, "%" + kw + "%"),
                        cb.exists(buyerSq),
                        cb.exists(sellerSq)
                    ));
                }
                
                // 状态筛选（处理空字符串）
                if (status != null && !status.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("orderStatus"), status.trim()));
                }
                
                // 卖家可见性筛选（处理空字符串）
                if (sellerVisibility != null && !sellerVisibility.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("sellerVisibility"), sellerVisibility.trim()));
                }
                
                // 买家可见性筛选（处理空字符串）
                if (buyerVisibility != null && !buyerVisibility.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("buyerVisibility"), buyerVisibility.trim()));
                }
                
                return predicates.isEmpty() ? cb.conjunction() : 
                    cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            
            Page<Order> orderPage = orderRepository.findAll(spec, pageable);
            
            // ✅ 批量查询所有买家和卖家的 UserProfile（避免 N+1 查询）
            List<Order> orders = orderPage.getContent();
            Set<String> userIds = new HashSet<>();
            for (Order o : orders) {
                if (o.getBuyerId() != null) userIds.add(o.getBuyerId());
                if (o.getSellerId() != null) userIds.add(o.getSellerId());
            }
            
            Map<String, UserProfile> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
                profileMap = profiles.stream()
                        .collect(java.util.stream.Collectors.toMap(UserProfile::getUserId, profile -> profile));
            }
            
            // ✅ 转换为包含买家和卖家信息的简单对象
            final Map<String, UserProfile> finalProfileMap = profileMap;
            List<Map<String, Object>> orderList = orders.stream()
                    .map(o -> {
                        UserProfile buyerProfile = finalProfileMap.get(o.getBuyerId());
                        UserProfile sellerProfile = finalProfileMap.get(o.getSellerId());
                        UserProfileInternalDTO buyerProfileDTO = buyerProfile != null ? convertToUserProfileDTO(buyerProfile) : null;
                        UserProfileInternalDTO sellerProfileDTO = sellerProfile != null ? convertToUserProfileDTO(sellerProfile) : null;
                        return toSimpleOrderWithUsers(o, buyerProfileDTO, sellerProfileDTO);
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            PageResultVO<Map<String, Object>> result = new PageResultVO<>();
            result.setList(orderList);
            result.setTotal(orderPage.getTotalElements());
            result.setPages(orderPage.getTotalPages());
            result.setCurrent(page);
            result.setSize(size);
            
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取订单列表异常: error={}", e.getMessage(), e);
            throw new BusinessException("获取订单列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result getOrderById(String orderId) {
        try {
            Optional<Order> opt = orderRepository.findById(orderId);
            if (opt.isEmpty()) {
                throw new BusinessException("订单不存在");
            }
            Order order = opt.get();
            
            // ✅ 批量查询买家和卖家Profile（避免N+1查询）
            Set<String> userIds = new HashSet<>();
            if (order.getBuyerId() != null) userIds.add(order.getBuyerId());
            if (order.getSellerId() != null) userIds.add(order.getSellerId());
            
            Map<String, UserProfile> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
                profileMap = profiles.stream()
                        .collect(java.util.stream.Collectors.toMap(UserProfile::getUserId, profile -> profile));
            }
            
            UserProfile buyerProfile = profileMap.get(order.getBuyerId());
            UserProfile sellerProfile = profileMap.get(order.getSellerId());
            UserProfileInternalDTO buyerProfileDTO = buyerProfile != null ? convertToUserProfileDTO(buyerProfile) : null;
            UserProfileInternalDTO sellerProfileDTO = sellerProfile != null ? convertToUserProfileDTO(sellerProfile) : null;
            
            return Result.ok(toSimpleOrderWithUsers(order, buyerProfileDTO, sellerProfileDTO));
        } catch (Exception e) {
            log.error("获取订单信息异常: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new BusinessException("获取订单信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateOrderFields(String orderId, String status, String trackingNumber, String remark) {
        try {
            Optional<Order> opt = orderRepository.findById(orderId);
            if (opt.isEmpty()) {
                throw new BusinessException("订单不存在");
            }
            Order order = opt.get();
            if (status != null) order.setOrderStatus(status);
            if (trackingNumber != null) order.setTrackingNumber(trackingNumber);
            if (remark != null) order.setRemark(remark);
            orderRepository.save(order);
            return Result.ok("更新成功", toSimpleOrder(order));
        } catch (Exception e) {
            log.error("更新订单异常: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new BusinessException("更新订单失败: " + e.getMessage());
        }
    }
    
    // ✅ 新增：更新订单完整字段（包括状态和可见性）
    @Override
    public Result updateOrderFull(String orderId, Map<String, Object> payload) {
        try {
            Optional<Order> opt = orderRepository.findById(orderId);
            if (opt.isEmpty()) {
                throw new BusinessException("订单不存在");
            }
            Order order = opt.get();
            
            // 状态
            Object orderStatus = payload.get("orderStatus");
            if (orderStatus instanceof String && StringUtils.hasText((String) orderStatus)) {
                order.setOrderStatus(((String) orderStatus).trim());
            }
            
            // 物流单号
            Object trackingNumber = payload.get("trackingNumber");
            if (trackingNumber instanceof String && StringUtils.hasText((String) trackingNumber)) {
                order.setTrackingNumber(((String) trackingNumber).trim());
            }
            
            // 备注
            Object remark = payload.get("remark");
            if (remark instanceof String) {
                order.setRemark(((String) remark).trim());
            }
            
            // 卖家可见性
            Object sellerVisibility = payload.get("sellerVisibility");
            if (sellerVisibility instanceof String && StringUtils.hasText((String) sellerVisibility)) {
                String vis = ((String) sellerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC", "PRIVATE", "HIDDEN"));
                if (!allowedVis.contains(vis)) {
                    throw new BusinessException("非法的卖家可见性");
                }
                order.setSellerVisibility(vis);
            }
            
            // 买家可见性
            Object buyerVisibility = payload.get("buyerVisibility");
            if (buyerVisibility instanceof String && StringUtils.hasText((String) buyerVisibility)) {
                String vis = ((String) buyerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC", "PRIVATE", "HIDDEN"));
                if (!allowedVis.contains(vis)) {
                    throw new BusinessException("非法的买家可见性");
                }
                order.setBuyerVisibility(vis);
            }
            
            orderRepository.save(order);
            return Result.ok("订单更新成功", toSimpleOrder(order));
        } catch (Exception e) {
            log.error("完整更新订单异常: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new BusinessException("更新订单失败: " + e.getMessage());
        }
    }

    @Override
    public Result deleteOrder(String orderId) {
        try {
            if (!orderRepository.existsById(orderId)) {
                throw new BusinessException("订单不存在");
            }
            orderRepository.deleteById(orderId);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除订单异常: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new BusinessException("删除失败: " + e.getMessage());
        }
    }

    // ===================== 管理端最小CRUD：会话/消息 =====================
    @Override
    public Result listConversations(Integer page, Integer size, String keyword) {
        try {
            // ✅ 直接访问数据库，不需要通过FeignClient
            // 构建分页参数
            Sort sort = Sort.by(Sort.Direction.DESC, "lastMessageTime");
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);
            
            // 构建查询条件
            org.springframework.data.jpa.domain.Specification<Conversation> spec = (root, query, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                
                // 关键词搜索：会话ID、用户ID（处理空字符串）
                if (keyword != null && !keyword.trim().isEmpty()) {
                    String kw = keyword.trim().toLowerCase();
                    predicates.add(cb.or(
                        cb.like(cb.lower(root.get("conversationId")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("userId1")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("userId2")), "%" + kw + "%")
                    ));
                }
                
                return predicates.isEmpty() ? cb.conjunction() : 
                    cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            
            Page<Conversation> conversationPage = conversationRepository.findAll(spec, pageable);
            
            // ✅ 批量查询所有用户的 UserProfile（避免 N+1 查询）
            List<Conversation> conversations = conversationPage.getContent();
            Set<String> userIds = new HashSet<>();
            for (Conversation c : conversations) {
                if (c.getUserId1() != null) userIds.add(c.getUserId1());
                if (c.getUserId2() != null) userIds.add(c.getUserId2());
            }
            
            Map<String, UserProfile> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
                profileMap = profiles.stream()
                        .collect(java.util.stream.Collectors.toMap(UserProfile::getUserId, profile -> profile));
            }
            
            // ✅ 转换为包含用户信息的简单对象
            final Map<String, UserProfile> finalProfileMap = profileMap;
            List<Map<String, Object>> simpleList = conversations.stream()
                    .map(c -> {
                        UserProfile user1Profile = finalProfileMap.get(c.getUserId1());
                        UserProfile user2Profile = finalProfileMap.get(c.getUserId2());
                        // 转换为UserProfileInternalDTO格式（兼容现有方法）
                        UserProfileInternalDTO user1DTO = user1Profile != null ? convertToUserProfileDTO(user1Profile) : null;
                        UserProfileInternalDTO user2DTO = user2Profile != null ? convertToUserProfileDTO(user2Profile) : null;
                        return toSimpleConversationWithUsers(c, user1DTO, user2DTO);
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", simpleList);
            result.put("total", conversationPage.getTotalElements());
            result.put("pages", conversationPage.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取会话列表异常: {}", e.getMessage(), e);
            throw new BusinessException("获取会话列表失败");
        }
    }

    @Override
    public Result getConversationById(String conversationId) {
        try {
            Optional<Conversation> opt = conversationRepository.findById(conversationId);
            if (opt.isEmpty()) {
                throw new BusinessException("会话不存在");
            }
            Conversation c = opt.get();
            
            // ✅ 批量查询用户Profile（避免N+1查询）
            Set<String> userIds = new HashSet<>();
            if (c.getUserId1() != null) userIds.add(c.getUserId1());
            if (c.getUserId2() != null) userIds.add(c.getUserId2());
            
            Map<String, UserProfile> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
                profileMap = profiles.stream()
                        .collect(java.util.stream.Collectors.toMap(UserProfile::getUserId, profile -> profile));
            }
            
            UserProfile user1Profile = profileMap.get(c.getUserId1());
            UserProfile user2Profile = profileMap.get(c.getUserId2());
            UserProfileInternalDTO user1DTO = user1Profile != null ? convertToUserProfileDTO(user1Profile) : null;
            UserProfileInternalDTO user2DTO = user2Profile != null ? convertToUserProfileDTO(user2Profile) : null;
            
            Map<String, Object> simpleResult = toSimpleConversationWithUsers(c, user1DTO, user2DTO);
            return Result.ok(simpleResult);
        } catch (Exception e) {
            log.error("获取会话详情异常: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw new BusinessException("获取会话详情失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateConversationFull(String conversationId, java.util.Map<String, Object> payload) {
        try {
            Optional<Conversation> opt = conversationRepository.findById(conversationId);
            if (opt.isEmpty()) {
                throw new BusinessException("会话不存在");
            }
            Conversation conversation = opt.get();
            
            // 更新字段
            Object status = payload.get("status");
            if (status instanceof String) conversation.setStatus((String) status);
            
            Object lastMessageContent = payload.get("lastMessageContent");
            if (lastMessageContent instanceof String) conversation.setLastMessageContent((String) lastMessageContent);
            
            Object lastMessageTime = payload.get("lastMessageTime");
            if (lastMessageTime instanceof String) {
                try {
                    conversation.setLastMessageTime(LocalDateTime.parse((String) lastMessageTime));
                } catch (Exception e) {
                    log.warn("解析lastMessageTime失败: conversationId={}, value={}, error={}", conversationId, lastMessageTime, e.getMessage());
                }
            }
            
            Object user1LastMessageContent = payload.get("user1LastMessageContent");
            if (user1LastMessageContent instanceof String) conversation.setUser1LastMessageContent((String) user1LastMessageContent);
            
            Object user1LastMessageTime = payload.get("user1LastMessageTime");
            if (user1LastMessageTime instanceof String) {
                try {
                    conversation.setUser1LastMessageTime(LocalDateTime.parse((String) user1LastMessageTime));
                } catch (Exception e) {
                    log.warn("解析user1LastMessageTime失败: conversationId={}, value={}, error={}", conversationId, user1LastMessageTime, e.getMessage());
                }
            }
            
            Object user2LastMessageContent = payload.get("user2LastMessageContent");
            if (user2LastMessageContent instanceof String) conversation.setUser2LastMessageContent((String) user2LastMessageContent);
            
            Object user2LastMessageTime = payload.get("user2LastMessageTime");
            if (user2LastMessageTime instanceof String) {
                try {
                    conversation.setUser2LastMessageTime(LocalDateTime.parse((String) user2LastMessageTime));
                } catch (Exception e) {
                    log.warn("解析user2LastMessageTime失败: conversationId={}, value={}, error={}", conversationId, user2LastMessageTime, e.getMessage());
                }
            }
            
            Object user1Count = payload.get("user1Count");
            if (user1Count instanceof Number) conversation.setUser1Count(((Number) user1Count).intValue());
            
            Object user2Count = payload.get("user2Count");
            if (user2Count instanceof Number) conversation.setUser2Count(((Number) user2Count).intValue());
            
            // ✅ 保存原来的可见性状态（用于检测是否恢复可见性）
            Boolean oldUser1Visibility = conversation.getUser1Visibility();
            Boolean oldUser2Visibility = conversation.getUser2Visibility();
            
            // 用户1可见性（Boolean类型）
            Boolean newUser1Visibility = null;
            Object user1Visibility = payload.get("user1Visibility");
            if (user1Visibility != null) {
                if (user1Visibility instanceof Boolean) {
                    newUser1Visibility = (Boolean) user1Visibility;
                    conversation.setUser1Visibility(newUser1Visibility);
                } else if (user1Visibility instanceof String) {
                    String v = ((String) user1Visibility).trim().toLowerCase();
                    newUser1Visibility = "true".equals(v) || "1".equals(v);
                    conversation.setUser1Visibility(newUser1Visibility);
                } else if (user1Visibility instanceof Number) {
                    newUser1Visibility = ((Number) user1Visibility).intValue() != 0;
                    conversation.setUser1Visibility(newUser1Visibility);
                }
            }
            
            // 用户2可见性（Boolean类型）
            Boolean newUser2Visibility = null;
            Object user2Visibility = payload.get("user2Visibility");
            if (user2Visibility != null) {
                if (user2Visibility instanceof Boolean) {
                    newUser2Visibility = (Boolean) user2Visibility;
                    conversation.setUser2Visibility(newUser2Visibility);
                } else if (user2Visibility instanceof String) {
                    String v = ((String) user2Visibility).trim().toLowerCase();
                    newUser2Visibility = "true".equals(v) || "1".equals(v);
                    conversation.setUser2Visibility(newUser2Visibility);
                } else if (user2Visibility instanceof Number) {
                    newUser2Visibility = ((Number) user2Visibility).intValue() != 0;
                    conversation.setUser2Visibility(newUser2Visibility);
                }
            }
            
            // ✅ 如果恢复可见性（从false变为true），需要更新对应用户的最后消息字段
            // 用户1恢复可见性
            if (newUser1Visibility != null && Boolean.FALSE.equals(oldUser1Visibility) && Boolean.TRUE.equals(newUser1Visibility)) {
                try {
                    // 查询用户1可见的最后一条消息
                    Pageable pageable = PageRequest.of(0, 1);
                    List<Message> lastMessages = 
                            messageRepository.findLastMessageForUser(conversationId, conversation.getUserId1(), pageable);
                    
                    if (!lastMessages.isEmpty()) {
                        Message lastMessage = lastMessages.get(0);
                        conversation.setUser1LastMessageContent(lastMessage.getContent());
                        conversation.setUser1LastMessageTime(lastMessage.getCreatedAt());
                    } else {
                        // 没有可见消息，设置为空
                        conversation.setUser1LastMessageContent(null);
                        conversation.setUser1LastMessageTime(null);
                    }
                } catch (Exception e) {
                    log.warn("管理端恢复用户1可见性时查询最后消息失败: conversationId={}, userId={}, error={}", 
                            conversationId, conversation.getUserId1(), e.getMessage());
                }
            }
            
            // 用户2恢复可见性
            if (newUser2Visibility != null && Boolean.FALSE.equals(oldUser2Visibility) && Boolean.TRUE.equals(newUser2Visibility)) {
                try {
                    // 查询用户2可见的最后一条消息
                    Pageable pageable = PageRequest.of(0, 1);
                    List<Message> lastMessages = 
                            messageRepository.findLastMessageForUser(conversationId, conversation.getUserId2(), pageable);
                    
                    if (!lastMessages.isEmpty()) {
                        Message lastMessage = lastMessages.get(0);
                        conversation.setUser2LastMessageContent(lastMessage.getContent());
                        conversation.setUser2LastMessageTime(lastMessage.getCreatedAt());
                    } else {
                        // 没有可见消息，设置为空
                        conversation.setUser2LastMessageContent(null);
                        conversation.setUser2LastMessageTime(null);
                    }
                } catch (Exception e) {
                    log.warn("管理端恢复用户2可见性时查询最后消息失败: conversationId={}, userId={}, error={}", 
                            conversationId, conversation.getUserId2(), e.getMessage());
                }
            }
            
            conversationRepository.save(conversation);
            return Result.ok("会话更新成功", toSimpleConversation(conversation));
        } catch (Exception e) {
            log.error("完整更新会话异常: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw new BusinessException("更新会话失败: " + e.getMessage());
        }
    }

    @Override
    public Result deleteConversation(String conversationId) {
        try {
            if (!conversationRepository.existsById(conversationId)) {
                throw new BusinessException("会话不存在");
            }
            conversationRepository.deleteById(conversationId);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除会话异常: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw new BusinessException("删除会话失败: " + e.getMessage());
        }
    }

    @Override
    public Result listMessages(String conversationId, Integer page, Integer size) {
        try {
            // ✅ 直接访问数据库，不需要通过FeignClient
            // 构建分页参数
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);
            
            // 构建查询条件
            org.springframework.data.jpa.domain.Specification<Message> spec = (root, query, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("conversationId"), conversationId));
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            
            Page<Message> messagePage = messageRepository.findAll(spec, pageable);
            
            // 统计双方都删除的消息数量（用于日志和验证）
            long bothDeletedCount = 0;
            List<Map<String, Object>> simpleList = new ArrayList<>();
            for (Message m : messagePage.getContent()) {
                // 统计双方都删除的消息
                if (Boolean.TRUE.equals(m.getDeletedBySender()) && Boolean.TRUE.equals(m.getDeletedByReceiver())) {
                    bothDeletedCount++;
                }
                simpleList.add(toSimpleMessage(m));
            }
            
            // 记录日志，验证是否查询到了双方都删除的消息
            log.debug("管理端查询消息列表: conversationId={}, page={}, size={}, total={}, bothDeletedCount={}", 
                    conversationId, page, size, messagePage.getTotalElements(), bothDeletedCount);
            
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("list", simpleList);
            resultMap.put("total", messagePage.getTotalElements());
            resultMap.put("pages", messagePage.getTotalPages());
            resultMap.put("current", page);
            resultMap.put("size", size);
            // 添加统计信息，方便前端了解有多少双方都删除的消息
            resultMap.put("bothDeletedCount", bothDeletedCount);
            return Result.ok(resultMap);
        } catch (Exception e) {
            log.error("获取消息列表异常: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw new BusinessException("获取消息列表失败: " + e.getMessage());
        }
    }

    // ===================== 简化映射，避免循环引用 =====================
    /**
     * 将管理员实体转换为简单VO（不包含密码）
     */
    private AdminSimpleVO toSimpleAdmin(Admin admin) {
        AdminSimpleVO vo = new AdminSimpleVO();
        vo.setAdminId(admin.getAdminId());
        vo.setUsername(admin.getUsername());
        vo.setRealName(admin.getRealName());
        vo.setEmail(admin.getEmail());
        vo.setDepartment(admin.getDepartment());
        vo.setPosition(admin.getPosition());
        vo.setAdminLevel(admin.getAdminLevel());
        vo.setPermissions(admin.getPermissions());
        vo.setAccountStatus(admin.getAccountStatus());
        vo.setCreateTime(admin.getCreateTime());
        vo.setUpdateTime(admin.getUpdateTime());
        vo.setLastLoginTime(admin.getLastLoginTime());
        vo.setLastLoginIp(admin.getLastLoginIp());
        vo.setLoginCount(admin.getLoginCount());
        vo.setRemark(admin.getRemark());
        // ✅ 不包含密码字段
        return vo;
    }

    /**
     * 将UserProfile转换为UserProfileInternalDTO
     */
    private UserProfileInternalDTO convertToUserProfileDTO(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        UserProfileInternalDTO dto = new UserProfileInternalDTO();
        dto.setUserId(profile.getUserId());
        dto.setNickname(profile.getNickname());
        dto.setAvatar(profile.getAvatar());
        dto.setCreditScore(profile.getCreditScore());
        dto.setBuyerRating(profile.getBuyerRating());
        dto.setSellerRating(profile.getSellerRating());
        // UserProfileInternalDTO可能没有这些字段，暂时注释
        // dto.setTotalSales(profile.getTotalSales());
        // dto.setTotalPurchases(profile.getTotalPurchases());
        // dto.setVipLevel(profile.getVipLevel());
        return dto;
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
            p.put("avatar", normalizeImageUrl(u.getUserProfile().getAvatar()));
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
    
    /**
     * 标准化图片URL，确保通过Gateway访问
     * 处理各种URL格式：
     * 1. 完整URL（http://localhost:8095/...）-> 转换为Gateway地址（http://localhost:8080/...）
     * 2. 完整URL（http://localhost:8080/...）-> 直接返回
     * 3. 相对路径（/api/images/...）-> 添加Gateway地址前缀
     * 4. 文件名（xxx.jpg）-> 根据路径特征判断是头像还是商品图片，构建完整URL
     */
    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        String url = imageUrl.trim();
        
        // 如果已经是完整URL
        if (url.startsWith("http://") || url.startsWith("https://")) {
            // 如果URL指向图片服务的直接端口（8095），需要转换为Gateway地址（8080）
            if (url.contains(":8095/")) {
                url = url.replace(":8095/", ":8080/");
            }
            // 如果URL已经是Gateway地址（8080），直接返回
            if (url.contains(":8080/")) {
                return url;
            }
            // 其他完整URL（如外部URL），直接返回
            return url;
        }
        
        // 如果是相对路径（包含/api/images/），添加Gateway地址前缀
        if (url.startsWith("/api/images/")) {
            return "http://localhost:8080" + url;
        }
        
        // 如果是文件名，构建完整URL（通过Gateway访问）
        String fileName = url;
        // 如果包含路径分隔符，提取文件名
        if (url.contains("/")) {
            fileName = url.substring(url.lastIndexOf("/") + 1);
        }
        
        // 判断是头像还是商品图片（根据文件名特征或路径）
        // 优先检查路径，如果路径中包含avatar相关关键词，则认为是头像
        if (url.contains("avatar") || url.contains("avatars")) {
            return "http://localhost:8080/api/images/avatars/" + fileName;
        } else {
            // 默认认为是商品图片（如果无法判断，也作为商品图片处理）
            return "http://localhost:8080/api/images/commodities/" + fileName;
        }
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
            sellerInfo.put("avatar", normalizeImageUrl(sellerProfile.getAvatar()));
            m.put("seller", sellerInfo);
        }
        
        return m;
    }

    @Override
    public Result updateCommodityFull(String commodityId, Map<String, Object> payload) {
        try {
            Optional<Commodity> opt = commodityRepository.findById(commodityId);
            if (opt.isEmpty()) {
                throw new BusinessException("商品不存在");
            }
            Commodity commodity = opt.get();
            
            // 更新字段
            Object title = payload.get("title");
            if (title instanceof String && StringUtils.hasText((String) title)) {
                commodity.setTitle(((String) title).trim());
            }
            
            Object description = payload.get("description");
            if (description instanceof String && StringUtils.hasText((String) description)) {
                commodity.setDescription(((String) description).trim());
            }
            
            // ✅ 价格：支持 Number 和 String 类型
            Object price = payload.get("price");
            if (price != null) {
                try {
                    double priceValue = price instanceof Number 
                        ? ((Number) price).doubleValue() 
                        : Double.parseDouble(price.toString().trim());
                    if (priceValue >= 0) {
                        commodity.setPrice(priceValue);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            // ✅ 库存：支持 Number 和 String 类型
            Object stock = payload.get("stock");
            if (stock != null) {
                try {
                    int stockValue = stock instanceof Number 
                        ? ((Number) stock).intValue() 
                        : Integer.parseInt(stock.toString().trim());
                    if (stockValue >= 0) {
                        commodity.setStock(stockValue);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            Object location = payload.get("location");
            if (location instanceof String && StringUtils.hasText((String) location)) {
                commodity.setLocation(((String) location).trim());
            }
            
            Object category = payload.get("category");
            if (category instanceof String && StringUtils.hasText((String) category)) {
                String cat = ((String) category).trim();
                java.util.Set<String> allowedCat = new java.util.HashSet<>(java.util.Arrays.asList(
                    "电子产品","服装配饰","图书文具","生活用品","运动户外","美妆护肤","其他"
                ));
                if (!allowedCat.contains(cat)) {
                    throw new BusinessException("非法的商品分类");
                }
                commodity.setCategory(cat);
            }
            
            Object conditionLevel = payload.get("conditionLevel");
            if (conditionLevel instanceof String && StringUtils.hasText((String) conditionLevel)) {
                String lvl = ((String) conditionLevel).trim();
                java.util.Set<String> allowedLvl = new java.util.HashSet<>(java.util.Arrays.asList(
                    "全新","九成新","八成新","七成新","六成新","五成新"
                ));
                if (!allowedLvl.contains(lvl)) {
                    throw new BusinessException("非法的成色等级");
                }
                commodity.setConditionLevel(lvl);
            }
            
            Object commodityStatus = payload.get("commodityStatus");
            if (commodityStatus instanceof String && StringUtils.hasText((String) commodityStatus)) {
                String st = ((String) commodityStatus).trim();
                java.util.Set<String> allowedStatus = new java.util.HashSet<>(java.util.Arrays.asList("DRAFT","PUBLISHED","ON_SHELF","OFF_SHELF"));
                if (!allowedStatus.contains(st)) {
                    throw new BusinessException("非法的商品状态");
                }
                commodity.setCommodityStatus(st);
            }
            
            // ✅ 更新商品图片（images字段，支持String和List类型）
            Object images = payload.get("images");
            if (images != null) {
                if (images instanceof String && StringUtils.hasText((String) images)) {
                    commodity.setImages(((String) images).trim());
                } else if (images instanceof List) {
                    // 如果是List，转换为JSON字符串
                    try {
                        String imagesJson = objectMapper.writeValueAsString(images);
                        commodity.setImages(imagesJson);
                    } catch (Exception e) {
                        log.warn("转换商品图片列表为JSON失败: commodityId={}, error={}", commodityId, e.getMessage());
                    }
                }
            }
            
            // ✅ 点击量：支持 Number 和 String 类型
            Object clickCount = payload.get("clickCount");
            if (clickCount != null) {
                try {
                    int count = clickCount instanceof Number 
                        ? ((Number) clickCount).intValue() 
                        : Integer.parseInt(clickCount.toString().trim());
                    if (count >= 0) {
                        commodity.setClickCount(count);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            // 可见性（允许编辑）
            Object sellerVisibility = payload.get("sellerVisibility");
            if (sellerVisibility instanceof String && StringUtils.hasText((String) sellerVisibility)) {
                String vis = ((String) sellerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC","PRIVATE","HIDDEN"));
                if (!allowedVis.contains(vis)) {
                    throw new BusinessException("非法的卖家可见性");
                }
                commodity.setSellerVisibility(vis);
            }
            
            Object buyerVisibility = payload.get("buyerVisibility");
            if (buyerVisibility instanceof String && StringUtils.hasText((String) buyerVisibility)) {
                String vis = ((String) buyerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC","PRIVATE","HIDDEN"));
                if (!allowedVis.contains(vis)) {
                    throw new BusinessException("非法的买家可见性");
                }
                commodity.setBuyerVisibility(vis);
            }
            
            // 不允许编辑：publishTime、reportCount、sellerId、commodityId
            
            commodityRepository.save(commodity);
            return Result.ok("更新成功", toSimpleCommodity(commodity));
        } catch (Exception e) {
            log.error("完整更新商品异常: commodityId={}, error={}", commodityId, e.getMessage(), e);
            throw new BusinessException("更新失败: " + e.getMessage());
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
            buyerInfo.put("avatar", normalizeImageUrl(buyerProfile.getAvatar()));
            m.put("buyer", buyerInfo);
        }
        
        // ✅ 添加卖家信息（如果提供了Profile）
        if (sellerProfile != null) {
            Map<String, Object> sellerInfo = new HashMap<>();
            sellerInfo.put("userId", o.getSellerId());
            sellerInfo.put("nickname", sellerProfile.getNickname());
            sellerInfo.put("avatar", normalizeImageUrl(sellerProfile.getAvatar()));
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
            user1Info.put("avatar", normalizeImageUrl(user1Profile.getAvatar()));
            m.put("user1", user1Info);
        }
        
        // ✅ 添加用户2信息（如果提供了Profile）
        if (user2Profile != null) {
            Map<String, Object> user2Info = new HashMap<>();
            user2Info.put("userId", c.getUserId2());
            user2Info.put("nickname", user2Profile.getNickname());
            user2Info.put("avatar", normalizeImageUrl(user2Profile.getAvatar()));
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
        m.put("imageUrl", normalizeImageUrl(m0.getImageUrl()));
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
            Optional<Message> opt = messageRepository.findById(messageId);
            if (opt.isEmpty()) {
                throw new BusinessException("消息不存在");
            }
            return Result.ok(toSimpleMessage(opt.get()));
        } catch (Exception e) {
            log.error("获取消息详情异常: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new BusinessException("获取消息详情失败: " + e.getMessage());
        }
    }

    @Override
    public Result updateMessageFull(String messageId, java.util.Map<String, Object> payload) {
        try {
            Optional<Message> opt = messageRepository.findById(messageId);
            if (opt.isEmpty()) {
                throw new BusinessException("消息不存在");
            }
            Message message = opt.get();
            String conversationId = message.getConversationId();
            
            // ✅ 保存原来的可见性状态（用于检测变化）
            Boolean oldDeletedBySender = message.getDeletedBySender();
            Boolean oldDeletedByReceiver = message.getDeletedByReceiver();
            
            // ✅ 获取对话信息（用于更新用户级别的最后消息字段）
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            
            // 更新字段
            Object content = payload.get("content");
            if (content instanceof String) message.setContent((String) content);
            
            Object messageType = payload.get("messageType");
            if (messageType instanceof String) message.setMessageType((String) messageType);
            
            Object imageUrl = payload.get("imageUrl");
            if (imageUrl instanceof String) message.setImageUrl((String) imageUrl);
            
            Object commodityId = payload.get("commodityId");
            if (commodityId instanceof String) message.setCommodityId((String) commodityId);
            
            Object orderId = payload.get("orderId");
            if (orderId instanceof String) message.setOrderId((String) orderId);
            
            // 已读状态（Boolean类型）
            Object isRead = payload.get("isRead");
            if (isRead != null) {
                if (isRead instanceof Boolean) {
                    message.setIsRead((Boolean) isRead);
                } else if (isRead instanceof String) {
                    String v = ((String) isRead).trim().toLowerCase();
                    message.setIsRead("true".equals(v) || "1".equals(v));
                } else if (isRead instanceof Number) {
                    message.setIsRead(((Number) isRead).intValue() != 0);
                }
            }
            
            Object readTime = payload.get("readTime");
            if (readTime instanceof String) {
                try {
                    message.setReadTime(LocalDateTime.parse((String) readTime));
                } catch (Exception e) {
                    log.warn("解析readTime失败: messageId={}, value={}, error={}", messageId, readTime, e.getMessage());
                }
            }
            
            // 发送方删除标记（Boolean类型）
            Boolean newDeletedBySender = null;
            Object deletedBySender = payload.get("deletedBySender");
            if (deletedBySender != null) {
                if (deletedBySender instanceof Boolean) {
                    newDeletedBySender = (Boolean) deletedBySender;
                    message.setDeletedBySender(newDeletedBySender);
                } else if (deletedBySender instanceof String) {
                    String v = ((String) deletedBySender).trim().toLowerCase();
                    newDeletedBySender = "true".equals(v) || "1".equals(v);
                    message.setDeletedBySender(newDeletedBySender);
                } else if (deletedBySender instanceof Number) {
                    newDeletedBySender = ((Number) deletedBySender).intValue() != 0;
                    message.setDeletedBySender(newDeletedBySender);
                }
            }
            
            // 接收方删除标记（Boolean类型）
            Boolean newDeletedByReceiver = null;
            Object deletedByReceiver = payload.get("deletedByReceiver");
            if (deletedByReceiver != null) {
                if (deletedByReceiver instanceof Boolean) {
                    newDeletedByReceiver = (Boolean) deletedByReceiver;
                    message.setDeletedByReceiver(newDeletedByReceiver);
                } else if (deletedByReceiver instanceof String) {
                    String v = ((String) deletedByReceiver).trim().toLowerCase();
                    newDeletedByReceiver = "true".equals(v) || "1".equals(v);
                    message.setDeletedByReceiver(newDeletedByReceiver);
                } else if (deletedByReceiver instanceof Number) {
                    newDeletedByReceiver = ((Number) deletedByReceiver).intValue() != 0;
                    message.setDeletedByReceiver(newDeletedByReceiver);
                }
            }
            
            // ✅ 保存消息的更改
            messageRepository.save(message);
            
            // ✅ 如果可见性发生变化，需要更新相关用户的最后消息字段
            if (convOpt.isPresent() && (newDeletedBySender != null || newDeletedByReceiver != null)) {
                // ✅ 重新从数据库加载conversation，确保获取最新的最后消息字段
                Conversation conversation = conversationRepository.findById(conversationId)
                        .orElseThrow(() -> new RuntimeException("对话不存在: " + conversationId));
                String senderId = message.getSenderId();
                String receiverId = message.getReceiverId();
                
                log.debug("管理端更新消息可见性: messageId={}, conversationId={}, oldDeletedBySender={}, newDeletedBySender={}, oldDeletedByReceiver={}, newDeletedByReceiver={}", 
                        messageId, conversationId, oldDeletedBySender, newDeletedBySender, oldDeletedByReceiver, newDeletedByReceiver);
                
                // 检查发送方可见性的变化
                if (newDeletedBySender != null && !newDeletedBySender.equals(oldDeletedBySender)) {
                    // 发送方可见性发生了变化
                    boolean wasVisible = !Boolean.TRUE.equals(oldDeletedBySender);
                    boolean isNowVisible = !Boolean.TRUE.equals(newDeletedBySender);
                    
                    log.debug("发送方可见性变化: wasVisible={}, isNowVisible={}, senderId={}", wasVisible, isNowVisible, senderId);
                    
                    if (wasVisible && !isNowVisible) {
                        // 从可见变为不可见（标记删除）
                        // 检查是否是发送方的最后一条可见消息
                        String senderLastContent = conversation.getLastMessageContentForUser(senderId);
                        LocalDateTime senderLastTime = conversation.getLastMessageTimeForUser(senderId);
                        // ✅ 使用更宽松的比较：内容相同，时间相差不超过1秒（处理精度问题）
                        boolean isSenderLastMessage = senderLastContent != null && senderLastTime != null &&
                            message.getContent().equals(senderLastContent) && 
                            (message.getCreatedAt().equals(senderLastTime) || 
                             Math.abs(java.time.Duration.between(message.getCreatedAt(), senderLastTime).getSeconds()) <= 1);
                        
                        log.debug("检查发送方最后消息: senderLastContent={}, senderLastTime={}, messageContent={}, messageCreatedAt={}, isSenderLastMessage={}", 
                                senderLastContent, senderLastTime, message.getContent(), message.getCreatedAt(), isSenderLastMessage);
                        
                        if (isSenderLastMessage) {
                            // 查询发送方可见的最后一条消息（因为当前消息已被标记删除，查询时会自动过滤）
                            try {
                                Pageable pageable = PageRequest.of(0, 1);
                                List<Message> lastMessages = 
                                        messageRepository.findLastMessageForUser(conversationId, senderId, pageable);
                                
                                log.debug("查询发送方可见消息结果: conversationId={}, senderId={}, foundCount={}", 
                                        conversationId, senderId, lastMessages.size());
                                
                                if (!lastMessages.isEmpty()) {
                                    Message newLastMessage = lastMessages.get(0);
                                    conversation.setLastMessageForUser(senderId, 
                                            newLastMessage.getContent(), 
                                            newLastMessage.getCreatedAt());
                                    log.info("管理端更新发送方最后消息: conversationId={}, senderId={}, newLastContent={}, newLastTime={}", 
                                            conversationId, senderId, newLastMessage.getContent(), newLastMessage.getCreatedAt());
                                } else {
                                    conversation.setLastMessageForUser(senderId, null, null);
                                    log.info("管理端清空发送方最后消息: conversationId={}, senderId={}", conversationId, senderId);
                                }
                            } catch (Exception e) {
                                log.warn("管理端更新消息可见性时更新发送方最后消息失败: conversationId={}, messageId={}, error={}", 
                                        conversationId, messageId, e.getMessage(), e);
                            }
                        } else {
                            log.debug("不是发送方的最后一条可见消息，无需更新: conversationId={}, senderId={}", conversationId, senderId);
                        }
                    } else if (!wasVisible && isNowVisible) {
                        // 从不可见变为可见（取消删除标记）
                        // 检查这条消息是否比当前最后消息更新
                        LocalDateTime senderLastTime = conversation.getLastMessageTimeForUser(senderId);
                        if (senderLastTime == null || message.getCreatedAt().isAfter(senderLastTime)) {
                            // 这条消息更新，更新为这条消息
                            conversation.setLastMessageForUser(senderId, 
                                    message.getContent(), 
                                    message.getCreatedAt());
                            log.info("管理端恢复发送方最后消息: conversationId={}, senderId={}, messageContent={}, messageCreatedAt={}", 
                                    conversationId, senderId, message.getContent(), message.getCreatedAt());
                        } else {
                            log.debug("消息不比当前最后消息更新，无需更新: conversationId={}, senderId={}, messageTime={}, lastTime={}", 
                                    conversationId, senderId, message.getCreatedAt(), senderLastTime);
                        }
                    }
                }
                
                // 检查接收方可见性的变化
                if (newDeletedByReceiver != null && !newDeletedByReceiver.equals(oldDeletedByReceiver)) {
                    // 接收方可见性发生了变化
                    boolean wasVisible = !Boolean.TRUE.equals(oldDeletedByReceiver);
                    boolean isNowVisible = !Boolean.TRUE.equals(newDeletedByReceiver);
                    
                    log.debug("接收方可见性变化: wasVisible={}, isNowVisible={}, receiverId={}", wasVisible, isNowVisible, receiverId);
                    
                    if (wasVisible && !isNowVisible) {
                        // 从可见变为不可见（标记删除）
                        // 检查是否是接收方的最后一条可见消息
                        String receiverLastContent = conversation.getLastMessageContentForUser(receiverId);
                        LocalDateTime receiverLastTime = conversation.getLastMessageTimeForUser(receiverId);
                        // ✅ 使用更宽松的比较：内容相同，时间相差不超过1秒（处理精度问题）
                        boolean isReceiverLastMessage = receiverLastContent != null && receiverLastTime != null &&
                            message.getContent().equals(receiverLastContent) && 
                            (message.getCreatedAt().equals(receiverLastTime) || 
                             Math.abs(java.time.Duration.between(message.getCreatedAt(), receiverLastTime).getSeconds()) <= 1);
                        
                        log.debug("检查接收方最后消息: receiverLastContent={}, receiverLastTime={}, messageContent={}, messageCreatedAt={}, isReceiverLastMessage={}", 
                                receiverLastContent, receiverLastTime, message.getContent(), message.getCreatedAt(), isReceiverLastMessage);
                        
                        if (isReceiverLastMessage) {
                            // 查询接收方可见的最后一条消息（因为当前消息已被标记删除，查询时会自动过滤）
                            try {
                                Pageable pageable = PageRequest.of(0, 1);
                                List<Message> lastMessages = 
                                        messageRepository.findLastMessageForUser(conversationId, receiverId, pageable);
                                
                                log.debug("查询接收方可见消息结果: conversationId={}, receiverId={}, foundCount={}", 
                                        conversationId, receiverId, lastMessages.size());
                                
                                if (!lastMessages.isEmpty()) {
                                    Message newLastMessage = lastMessages.get(0);
                                    conversation.setLastMessageForUser(receiverId, 
                                            newLastMessage.getContent(), 
                                            newLastMessage.getCreatedAt());
                                    log.info("管理端更新接收方最后消息: conversationId={}, receiverId={}, newLastContent={}, newLastTime={}", 
                                            conversationId, receiverId, newLastMessage.getContent(), newLastMessage.getCreatedAt());
                                } else {
                                    conversation.setLastMessageForUser(receiverId, null, null);
                                    log.info("管理端清空接收方最后消息: conversationId={}, receiverId={}", conversationId, receiverId);
                                }
                            } catch (Exception e) {
                                log.warn("管理端更新消息可见性时更新接收方最后消息失败: conversationId={}, messageId={}, error={}", 
                                        conversationId, messageId, e.getMessage(), e);
                            }
                        } else {
                            log.debug("不是接收方的最后一条可见消息，无需更新: conversationId={}, receiverId={}", conversationId, receiverId);
                        }
                    } else if (!wasVisible && isNowVisible) {
                        // 从不可见变为可见（取消删除标记）
                        // 检查这条消息是否比当前最后消息更新
                        LocalDateTime receiverLastTime = conversation.getLastMessageTimeForUser(receiverId);
                        if (receiverLastTime == null || message.getCreatedAt().isAfter(receiverLastTime)) {
                            // 这条消息更新，更新为这条消息
                            conversation.setLastMessageForUser(receiverId, 
                                    message.getContent(), 
                                    message.getCreatedAt());
                            log.info("管理端恢复接收方最后消息: conversationId={}, receiverId={}, messageContent={}, messageCreatedAt={}", 
                                    conversationId, receiverId, message.getContent(), message.getCreatedAt());
                        } else {
                            log.debug("消息不比当前最后消息更新，无需更新: conversationId={}, receiverId={}, messageTime={}, lastTime={}", 
                                    conversationId, receiverId, message.getCreatedAt(), receiverLastTime);
                        }
                    }
                }
                
                // 保存对话的更新
                conversationRepository.save(conversation);
                log.debug("管理端保存对话更新: conversationId={}", conversationId);
            } else {
                log.debug("管理端更新消息可见性: 条件不满足，跳过更新最后消息 - convOpt.isPresent()={}, newDeletedBySender={}, newDeletedByReceiver={}", 
                        convOpt.isPresent(), newDeletedBySender, newDeletedByReceiver);
            }
            
            return Result.ok("消息更新成功", toSimpleMessage(message));
        } catch (Exception e) {
            log.error("完整更新消息异常: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new BusinessException("更新消息失败");
        }
    }

    @Override
    public Result deleteMessage(String messageId) {
        try {
            Optional<Message> msgOpt = messageRepository.findById(messageId);
            if (msgOpt.isEmpty()) {
                throw new BusinessException("消息不存在");
            }
            
            Message message = msgOpt.get();
            String conversationId = message.getConversationId();
            
            // ✅ 管理端硬删除消息前，检查并更新相关用户的最后消息字段
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (convOpt.isPresent()) {
                Conversation conversation = convOpt.get();
                
                // ✅ 检查消息对用户1是否可见，以及是否是最后一条可见消息
                String user1Id = conversation.getUserId1();
                boolean isVisibleToUser1 = false;
                if (user1Id != null && user1Id.equals(message.getSenderId())) {
                    // 用户1是发送方，检查是否被发送方删除
                    isVisibleToUser1 = !Boolean.TRUE.equals(message.getDeletedBySender());
                } else if (user1Id != null && user1Id.equals(message.getReceiverId())) {
                    // 用户1是接收方，检查是否被接收方删除
                    isVisibleToUser1 = !Boolean.TRUE.equals(message.getDeletedByReceiver());
                }
                
                String user1LastContent = conversation.getUser1LastMessageContent();
                LocalDateTime user1LastTime = conversation.getUser1LastMessageTime();
                boolean isUser1LastMessage = isVisibleToUser1 && 
                    user1LastContent != null && user1LastTime != null &&
                    message.getContent().equals(user1LastContent) && 
                    message.getCreatedAt().equals(user1LastTime);
                
                // ✅ 检查消息对用户2是否可见，以及是否是最后一条可见消息
                String user2Id = conversation.getUserId2();
                boolean isVisibleToUser2 = false;
                if (user2Id != null && user2Id.equals(message.getSenderId())) {
                    // 用户2是发送方，检查是否被发送方删除
                    isVisibleToUser2 = !Boolean.TRUE.equals(message.getDeletedBySender());
                } else if (user2Id != null && user2Id.equals(message.getReceiverId())) {
                    // 用户2是接收方，检查是否被接收方删除
                    isVisibleToUser2 = !Boolean.TRUE.equals(message.getDeletedByReceiver());
                }
                
                String user2LastContent = conversation.getUser2LastMessageContent();
                LocalDateTime user2LastTime = conversation.getUser2LastMessageTime();
                boolean isUser2LastMessage = isVisibleToUser2 && 
                    user2LastContent != null && user2LastTime != null &&
                    message.getContent().equals(user2LastContent) && 
                    message.getCreatedAt().equals(user2LastTime);
                
                // ✅ 如果删除的是某个用户的最后一条可见消息，需要更新对应用户字段
                // 注意：删除前查询会包含这条消息，所以需要查询2条，跳过第一条
                if (isUser1LastMessage) {
                    try {
                        // 查询用户1可见的前2条消息，第一条是要删除的，取第二条
                        Pageable pageable = PageRequest.of(0, 2);
                        List<Message> lastMessages = 
                                messageRepository.findLastMessageForUser(
                                        conversationId, conversation.getUserId1(), pageable);
                        
                        if (lastMessages.size() > 1) {
                            // 有第二条消息，使用第二条作为新的最后消息
                            Message newLastMessage = lastMessages.get(1);
                            conversation.setUser1LastMessageContent(newLastMessage.getContent());
                            conversation.setUser1LastMessageTime(newLastMessage.getCreatedAt());
                        } else {
                            // 没有其他可见消息，设置为空
                            conversation.setUser1LastMessageContent(null);
                            conversation.setUser1LastMessageTime(null);
                        }
                    } catch (Exception e) {
                        log.warn("管理端硬删除消息时更新用户1最后消息失败: conversationId={}, messageId={}, error={}", 
                                conversationId, messageId, e.getMessage());
                    }
                }
                
                if (isUser2LastMessage) {
                    try {
                        // 查询用户2可见的前2条消息，第一条是要删除的，取第二条
                        Pageable pageable = PageRequest.of(0, 2);
                        List<Message> lastMessages = 
                                messageRepository.findLastMessageForUser(
                                        conversationId, conversation.getUserId2(), pageable);
                        
                        if (lastMessages.size() > 1) {
                            // 有第二条消息，使用第二条作为新的最后消息
                            Message newLastMessage = lastMessages.get(1);
                            conversation.setUser2LastMessageContent(newLastMessage.getContent());
                            conversation.setUser2LastMessageTime(newLastMessage.getCreatedAt());
                        } else {
                            // 没有其他可见消息，设置为空
                            conversation.setUser2LastMessageContent(null);
                            conversation.setUser2LastMessageTime(null);
                        }
                    } catch (Exception e) {
                        log.warn("管理端硬删除消息时更新用户2最后消息失败: conversationId={}, messageId={}, error={}", 
                                conversationId, messageId, e.getMessage());
                    }
                }
                
                // 保存对话的更新
                conversationRepository.save(conversation);
            }
            
            messageRepository.deleteById(messageId);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除消息异常: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new BusinessException("删除消息失败: " + e.getMessage());
        }
    }
}

