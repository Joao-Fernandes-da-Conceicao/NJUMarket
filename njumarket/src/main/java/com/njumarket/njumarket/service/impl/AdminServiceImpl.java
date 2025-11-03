package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.AdminLoginDTO;
import com.njumarket.njumarket.entity.Admin;
import com.njumarket.njumarket.repository.AdminRepository;
import com.njumarket.njumarket.repository.UserRepository;
import com.njumarket.njumarket.repository.CommodityRepository;
import com.njumarket.njumarket.repository.OrderRepository;
import com.njumarket.njumarket.repository.ConversationRepository;
import com.njumarket.njumarket.repository.MessageRepository;
import com.njumarket.njumarket.repository.UserProfileRepository;
import com.njumarket.njumarket.service.AdminService;
import com.njumarket.njumarket.service.PasswordService;
import com.njumarket.njumarket.utils.JwtUtils;
import com.njumarket.njumarket.utils.UserHolder;
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
    private final UserRepository userRepository;
    private final CommodityRepository commodityRepository;
    private final OrderRepository orderRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserProfileRepository userProfileRepository;

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
                //log.info("password: {}\n your password: {}", admin.getPassword(), passwordService.encodePassword(loginDTO.getPassword()));
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
            Admin admin = UserHolder.getAdmin();
            if (admin == null) {
                return Result.fail("管理员未登录");
            }
            
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
            Admin currentAdmin = com.njumarket.njumarket.utils.UserHolder.getAdmin();
            if (currentAdmin == null || !currentAdmin.isSystemAdmin()) {
                log.warn("非system权限管理员尝试创建管理员: adminId={}", 
                    currentAdmin != null ? currentAdmin.getAdminId() : "null");
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
            Admin currentAdmin = com.njumarket.njumarket.utils.UserHolder.getAdmin();
            if (currentAdmin == null || !currentAdmin.isSystemAdmin()) {
                log.warn("非system权限管理员尝试访问管理员列表: adminId={}", 
                    currentAdmin != null ? currentAdmin.getAdminId() : "null");
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
                    // 使用自定义排序：null值排在最后（升序时），或最前（降序时）
                    // JPA原生不支持COALESCE，这里先按字段排序，null值会被JPA自动处理
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
            Admin currentAdmin = com.njumarket.njumarket.utils.UserHolder.getAdmin();
            if (currentAdmin == null || !currentAdmin.isSystemAdmin()) {
                log.warn("非system权限管理员尝试查看管理员信息: adminId={}, targetAdminId={}", 
                    currentAdmin != null ? currentAdmin.getAdminId() : "null", adminId);
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
            Admin currentAdmin = com.njumarket.njumarket.utils.UserHolder.getAdmin();
            if (currentAdmin == null || !currentAdmin.isSystemAdmin()) {
                log.warn("非system权限管理员尝试更新管理员信息: adminId={}, targetAdminId={}", 
                    currentAdmin != null ? currentAdmin.getAdminId() : "null", adminId);
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
            String kw = keyword == null ? "" : keyword.trim().toLowerCase();
            org.springframework.data.jpa.domain.Specification<com.njumarket.njumarket.entity.User> spec = (root, query, cb) -> {
                java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
                if (!kw.isEmpty()) {
                    jakarta.persistence.criteria.Expression<String> uname = cb.lower(root.get("username"));
                    jakarta.persistence.criteria.Expression<String> phone = cb.lower(root.get("primaryPhone"));
                    predicates.add(cb.or(cb.like(uname, "%" + kw + "%"), cb.like(phone, "%" + kw + "%")));
                }
                if (org.springframework.util.StringUtils.hasText(accountStatus)) {
                    String v = accountStatus.trim().toLowerCase();
                    predicates.add(cb.equal(cb.lower(root.get("accountStatus")), v));
                }
                return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            // 排序
            Sort sort = Sort.by(Sort.Direction.DESC, "registerTime");
            if (org.springframework.util.StringUtils.hasText(sortProp)) {
                String sp = sortProp.trim();
                Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
                if ("registerTime".equals(sp) || "username".equals(sp) || "primaryPhone".equals(sp)) {
                    sort = Sort.by(dir, sp);
                }
            }
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);
            Page<com.njumarket.njumarket.entity.User> userPage = userRepository.findAll(spec, pageable);
            List<Map<String, Object>> simpleList = new ArrayList<>();
            for (com.njumarket.njumarket.entity.User u : userPage.getContent()) {
                simpleList.add(toSimpleUser(u));
            }
            Map<String, Object> result = new HashMap<>();
            result.put("list", simpleList);
            result.put("total", userPage.getTotalElements());
            result.put("pages", userPage.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取用户列表异常: {}", e.getMessage());
            return Result.fail("获取用户列表失败");
        }
    }

    @Override
    public Result getUserById(String userId) {
        try {
            Optional<com.njumarket.njumarket.entity.User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return Result.fail("用户不存在");
            }
            com.njumarket.njumarket.entity.User user = userOpt.get();
            return Result.ok(toSimpleUser(user));
        } catch (Exception e) {
            log.error("获取用户信息异常: userId={}, error={}", userId, e.getMessage());
            return Result.fail("获取用户信息失败");
        }
    }

    @Override
    public Result updateUserStatus(String userId, String status) {
        try {
            Optional<com.njumarket.njumarket.entity.User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return Result.fail("用户不存在");
            }
            com.njumarket.njumarket.entity.User user = userOpt.get();
            // 校验账户状态
            if (status == null) return Result.fail("状态不能为空");
            String newStatus = status.trim();
            java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList("ACTIVE","SUSPENDED","BANNED"));
            if (!allowed.contains(newStatus)) {
                return Result.fail("非法的账户状态");
            }
            user.setAccountStatus(newStatus);
            userRepository.save(user);
            return Result.ok("用户状态更新成功");
        } catch (Exception e) {
            log.error("更新用户状态异常: userId={}, status={}, error={}", userId, status, e.getMessage());
            return Result.fail("更新用户状态失败");
        }
    }

    @Override
    public Result updateUserBasic(String userId, String nickname, String phone, String email) {
        try {
            Optional<com.njumarket.njumarket.entity.User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return Result.fail("用户不存在");
            }
            com.njumarket.njumarket.entity.User user = userOpt.get();
            if (org.springframework.util.StringUtils.hasText(phone)) {
                user.setPrimaryPhone(phone.trim());
            }
            // 更新昵称到UserProfile（若存在）
            if (user.getUserProfile() != null && org.springframework.util.StringUtils.hasText(nickname)) {
                user.getUserProfile().setNickname(nickname.trim());
            }
            // 目前未建统一email字段，暂不处理email，保留扩展点
            userRepository.save(user);
            return Result.ok("用户信息更新成功");
        } catch (Exception e) {
            log.error("更新用户基础信息异常: userId={}, error={}", userId, e.getMessage());
            return Result.fail("更新用户信息失败");
        }
    }

    @Override
    public Result deleteUser(String userId) {
        try {
            if (!userRepository.existsById(userId)) {
                return Result.fail("用户不存在");
            }
            // 优先软删：将账户状态置为DELETED
            com.njumarket.njumarket.entity.User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setAccountStatus("DELETED");
                userRepository.save(user);
            }
            return Result.ok("用户删除成功");
        } catch (Exception e) {
            log.error("删除用户异常: userId={}, error={}", userId, e.getMessage());
            return Result.fail("删除用户失败");
        }
    }

    @Override
    public Result updateUserFull(String userId, Map<String, Object> payload) {
        try {
            Optional<com.njumarket.njumarket.entity.User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return Result.fail("用户不存在");
            }
            com.njumarket.njumarket.entity.User user = userOpt.get();
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
                    return Result.fail("非法的账户状态");
                }
                user.setAccountStatus(newStatus);
            }

            // 档案字段
            com.njumarket.njumarket.entity.UserProfile profile = user.getUserProfile();
            if (profile == null) {
                profile = new com.njumarket.njumarket.entity.UserProfile();
                profile.setProfileId("PROFILE_" + System.currentTimeMillis());
                profile.setUserId(user.getUserId());
            }
            Object nickname = payload.get("nickname");
            if (nickname instanceof String) profile.setNickname(((String) nickname).trim());
            Object avatar = payload.get("avatar");
            if (avatar instanceof String) profile.setAvatar(((String) avatar).trim());
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
            if (vipLevel instanceof String) {
                String lvl = ((String) vipLevel).trim();
                java.util.Set<String> allowedLvl = new java.util.HashSet<>(java.util.Arrays.asList("NORMAL","BRONZE","SILVER","GOLD","PLATINUM"));
                if (!allowedLvl.contains(lvl)) {
                    return Result.fail("非法的会员等级");
                }
                profile.setVipLevel(lvl);
            }

            userRepository.save(user);
            userProfileRepository.save(profile);

            return Result.ok("更新成功", toSimpleUser(user));
        } catch (Exception e) {
            log.error("完整更新用户异常: userId={}, error={}", userId, e.getMessage());
            return Result.fail("更新失败");
        }
    }

    // ===================== 管理端最小CRUD：商品 =====================
    @Override
    public Result listCommodities(Integer page, Integer size, String keyword, String category, String conditionLevel, String status, String sellerVisibility, String buyerVisibility, String sortProp, String sortOrder) {
        try {
            String kw = keyword == null ? "" : keyword.trim().toLowerCase();
            org.springframework.data.jpa.domain.Specification<com.njumarket.njumarket.entity.Commodity> spec = (root, query, cb) -> {
                java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
                if (!kw.isEmpty()) {
                    jakarta.persistence.criteria.Expression<String> title = cb.lower(root.get("title"));
                    jakarta.persistence.criteria.Expression<String> sellerIdExp = cb.lower(root.get("sellerId"));
                    // 子查询：按卖家昵称（UserProfile.nickname）模糊匹配
                    jakarta.persistence.criteria.Subquery<com.njumarket.njumarket.entity.User> sq = query.subquery(com.njumarket.njumarket.entity.User.class);
                    jakarta.persistence.criteria.Root<com.njumarket.njumarket.entity.User> ur = sq.from(com.njumarket.njumarket.entity.User.class);
                    // LEFT JOIN userProfile
                    jakarta.persistence.criteria.Join<com.njumarket.njumarket.entity.User, com.njumarket.njumarket.entity.UserProfile> profileJoin = ur.join("userProfile", jakarta.persistence.criteria.JoinType.LEFT);
                    jakarta.persistence.criteria.Predicate sellerMatch = cb.equal(ur.get("userId"), root.get("sellerId"));
                    jakarta.persistence.criteria.Predicate nickLike = cb.like(cb.lower(profileJoin.get("nickname")), "%" + kw + "%");
                    sq.select(ur).where(cb.and(sellerMatch, nickLike));

                    predicates.add(cb.or(
                        cb.like(title, "%" + kw + "%"),
                        cb.like(sellerIdExp, "%" + kw + "%"),
                        cb.exists(sq)
                    ));
                }
                if (org.springframework.util.StringUtils.hasText(category)) {
                    predicates.add(cb.equal(root.get("category"), category.trim()));
                }
                if (org.springframework.util.StringUtils.hasText(conditionLevel)) {
                    predicates.add(cb.equal(root.get("conditionLevel"), conditionLevel.trim()));
                }
                if (org.springframework.util.StringUtils.hasText(status)) {
                    predicates.add(cb.equal(root.get("commodityStatus"), status.trim()));
                }
                if (org.springframework.util.StringUtils.hasText(sellerVisibility)) {
                    predicates.add(cb.equal(root.get("sellerVisibility"), sellerVisibility.trim()));
                }
                if (org.springframework.util.StringUtils.hasText(buyerVisibility)) {
                    predicates.add(cb.equal(root.get("buyerVisibility"), buyerVisibility.trim()));
                }
                return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            // 排序
            Sort sort = Sort.by(Sort.Direction.DESC, "publishTime");
            if (org.springframework.util.StringUtils.hasText(sortProp)) {
                String sp = sortProp.trim();
                Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
                if ("publishTime".equals(sp) || "clickCount".equals(sp) || "price".equals(sp)) {
                    sort = Sort.by(dir, sp);
                }
            }
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);
            Page<com.njumarket.njumarket.entity.Commodity> p = commodityRepository.findAll(spec, pageable);
            
            // ✅ 批量查询所有卖家的 UserProfile（避免 N+1 查询）
            List<com.njumarket.njumarket.entity.Commodity> commodities = p.getContent();
            Set<String> sellerIds = commodities.stream()
                    .map(com.njumarket.njumarket.entity.Commodity::getSellerId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            
            Map<String, com.njumarket.njumarket.entity.UserProfile> profileMap = new HashMap<>();
            if (!sellerIds.isEmpty()) {
                List<com.njumarket.njumarket.entity.UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(sellerIds));
                profileMap = profiles.stream()
                        .collect(java.util.stream.Collectors.toMap(com.njumarket.njumarket.entity.UserProfile::getUserId, profile -> profile));
            }
            
            // ✅ 转换为包含卖家信息的简单对象
            final Map<String, com.njumarket.njumarket.entity.UserProfile> finalProfileMap = profileMap;
            List<Map<String, Object>> simpleList = commodities.stream()
                    .map(c -> toSimpleCommodityWithSeller(c, finalProfileMap.get(c.getSellerId())))
                    .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", simpleList);
            result.put("total", p.getTotalElements());
            result.put("pages", p.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取商品列表异常: {}", e.getMessage());
            return Result.fail("获取商品列表失败");
        }
    }

    @Override
    public Result getCommodityById(String commodityId) {
        try {
            return commodityRepository.findById(commodityId)
                    .<Result>map(c -> Result.ok(toSimpleCommodity(c)))
                    .orElseGet(() -> Result.fail("商品不存在"));
        } catch (Exception e) {
            log.error("获取商品信息异常: commodityId={}, error={}", commodityId, e.getMessage());
            return Result.fail("获取商品信息失败");
        }
    }

    @Override
    public Result updateCommodityStatus(String commodityId, String status) {
        try {
            Optional<com.njumarket.njumarket.entity.Commodity> opt = commodityRepository.findById(commodityId);
            if (opt.isEmpty()) {
                return Result.fail("商品不存在");
            }
            com.njumarket.njumarket.entity.Commodity c = opt.get();
            c.setCommodityStatus(status);
            commodityRepository.save(c);
            return Result.ok("商品状态更新成功");
        } catch (Exception e) {
            log.error("更新商品状态异常: commodityId={}, status={}, error={}", commodityId, status, e.getMessage());
            return Result.fail("更新商品状态失败");
        }
    }

    @Override
    public Result deleteCommodity(String commodityId) {
        try {
            if (!commodityRepository.existsById(commodityId)) {
                return Result.fail("商品不存在");
            }
            commodityRepository.deleteById(commodityId);
            return Result.ok("商品删除成功");
        } catch (Exception e) {
            log.error("删除商品异常: commodityId={}, error={}", commodityId, e.getMessage());
            return Result.fail("删除商品失败");
        }
    }

    // ===================== 管理端最小CRUD：订单 =====================
    @Override
    public Result listOrders(Integer page, Integer size, String keyword, String status, String sellerVisibility, String buyerVisibility, String sortProp, String sortOrder) {
        try {
            String kw = keyword == null ? "" : keyword.trim().toLowerCase();
            org.springframework.data.jpa.domain.Specification<com.njumarket.njumarket.entity.Order> spec = (root, query, cb) -> {
                java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
                if (!kw.isEmpty()) {
                    // ✅ 搜索优化：支持买家ID、卖家ID、商品标题、买家昵称、卖家昵称
                    jakarta.persistence.criteria.Expression<String> buyerId = cb.lower(root.get("buyerId"));
                    jakarta.persistence.criteria.Expression<String> sellerId = cb.lower(root.get("sellerId"));
                    jakarta.persistence.criteria.Expression<String> snapTitle = cb.lower(root.get("commoditySnapshotTitle"));
                    
                    // 子查询：按买家昵称（UserProfile.nickname）模糊匹配
                    jakarta.persistence.criteria.Subquery<com.njumarket.njumarket.entity.User> buyerSq = query.subquery(com.njumarket.njumarket.entity.User.class);
                    jakarta.persistence.criteria.Root<com.njumarket.njumarket.entity.User> buyerUr = buyerSq.from(com.njumarket.njumarket.entity.User.class);
                    jakarta.persistence.criteria.Join<com.njumarket.njumarket.entity.User, com.njumarket.njumarket.entity.UserProfile> buyerProfileJoin = buyerUr.join("userProfile", jakarta.persistence.criteria.JoinType.LEFT);
                    jakarta.persistence.criteria.Predicate buyerMatch = cb.equal(buyerUr.get("userId"), root.get("buyerId"));
                    jakarta.persistence.criteria.Predicate buyerNickLike = cb.like(cb.lower(buyerProfileJoin.get("nickname")), "%" + kw + "%");
                    buyerSq.select(buyerUr).where(cb.and(buyerMatch, buyerNickLike));
                    
                    // 子查询：按卖家昵称（UserProfile.nickname）模糊匹配
                    jakarta.persistence.criteria.Subquery<com.njumarket.njumarket.entity.User> sellerSq = query.subquery(com.njumarket.njumarket.entity.User.class);
                    jakarta.persistence.criteria.Root<com.njumarket.njumarket.entity.User> sellerUr = sellerSq.from(com.njumarket.njumarket.entity.User.class);
                    jakarta.persistence.criteria.Join<com.njumarket.njumarket.entity.User, com.njumarket.njumarket.entity.UserProfile> sellerProfileJoin = sellerUr.join("userProfile", jakarta.persistence.criteria.JoinType.LEFT);
                    jakarta.persistence.criteria.Predicate sellerMatch = cb.equal(sellerUr.get("userId"), root.get("sellerId"));
                    jakarta.persistence.criteria.Predicate sellerNickLike = cb.like(cb.lower(sellerProfileJoin.get("nickname")), "%" + kw + "%");
                    sellerSq.select(sellerUr).where(cb.and(sellerMatch, sellerNickLike));
                    
                    predicates.add(cb.or(
                        cb.like(buyerId, "%" + kw + "%"),
                        cb.like(sellerId, "%" + kw + "%"),
                        cb.like(snapTitle, "%" + kw + "%"),
                        cb.exists(buyerSq),
                        cb.exists(sellerSq)
                    ));
                }
                if (org.springframework.util.StringUtils.hasText(status)) {
                    predicates.add(cb.equal(root.get("orderStatus"), status.trim()));
                }
                // ✅ 卖家可见性筛选
                if (org.springframework.util.StringUtils.hasText(sellerVisibility)) {
                    predicates.add(cb.equal(root.get("sellerVisibility"), sellerVisibility.trim()));
                }
                // ✅ 买家可见性筛选
                if (org.springframework.util.StringUtils.hasText(buyerVisibility)) {
                    predicates.add(cb.equal(root.get("buyerVisibility"), buyerVisibility.trim()));
                }
                return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            // 排序（默认 createTime desc）
            Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
            if (org.springframework.util.StringUtils.hasText(sortProp)) {
                String sp = sortProp.trim();
                Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
                // ✅ 支持创建时间和金额排序
                if ("createTime".equals(sp) || "payAmount".equals(sp)) {
                    sort = Sort.by(dir, sp);
                }
            }
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);
            Page<com.njumarket.njumarket.entity.Order> p = orderRepository.findAll(spec, pageable);
            
            // ✅ 批量查询所有买家和卖家的 UserProfile（避免 N+1 查询，为将来显示用户信息做准备）
            List<com.njumarket.njumarket.entity.Order> orders = p.getContent();
            Set<String> userIds = new HashSet<>();
            for (com.njumarket.njumarket.entity.Order o : orders) {
                if (o.getBuyerId() != null) userIds.add(o.getBuyerId());
                if (o.getSellerId() != null) userIds.add(o.getSellerId());
            }
            
            Map<String, com.njumarket.njumarket.entity.UserProfile> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<com.njumarket.njumarket.entity.UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
                profileMap = profiles.stream()
                        .collect(java.util.stream.Collectors.toMap(com.njumarket.njumarket.entity.UserProfile::getUserId, profile -> profile));
            }
            
            // ✅ 转换为包含用户信息的简单对象
            final Map<String, com.njumarket.njumarket.entity.UserProfile> finalProfileMap = profileMap;
            List<Map<String, Object>> simpleList = orders.stream()
                    .map(o -> toSimpleOrderWithUsers(o, 
                            finalProfileMap.get(o.getBuyerId()), 
                            finalProfileMap.get(o.getSellerId())))
                    .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", simpleList);
            result.put("total", p.getTotalElements());
            result.put("pages", p.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取订单列表异常: {}", e.getMessage());
            return Result.fail("获取订单列表失败");
        }
    }

    @Override
    public Result getOrderById(String orderId) {
        try {
            return orderRepository.findById(orderId)
                    .<Result>map(o -> Result.ok(toSimpleOrder(o)))
                    .orElseGet(() -> Result.fail("订单不存在"));
        } catch (Exception e) {
            log.error("获取订单信息异常: orderId={}, error={}", orderId, e.getMessage());
            return Result.fail("获取订单信息失败");
        }
    }

    @Override
    public Result updateOrderFields(String orderId, String status, String trackingNumber, String remark) {
        try {
            Optional<com.njumarket.njumarket.entity.Order> opt = orderRepository.findById(orderId);
            if (opt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            com.njumarket.njumarket.entity.Order order = opt.get();
            if (org.springframework.util.StringUtils.hasText(status)) {
                order.setOrderStatus(status);
            }
            if (org.springframework.util.StringUtils.hasText(trackingNumber)) {
                order.setTrackingNumber(trackingNumber.trim());
            }
            // remark 字段可能在DTO中，实体未必有；此处仅预留
            orderRepository.save(order);
            return Result.ok("订单更新成功");
        } catch (Exception e) {
            log.error("更新订单异常: orderId={}, error={}", orderId, e.getMessage());
            return Result.fail("更新订单失败");
        }
    }
    
    // ✅ 新增：更新订单完整字段（包括状态和可见性）
    @Override
    public Result updateOrderFull(String orderId, Map<String, Object> payload) {
        try {
            Optional<com.njumarket.njumarket.entity.Order> opt = orderRepository.findById(orderId);
            if (opt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            com.njumarket.njumarket.entity.Order order = opt.get();
            
            // 状态
            Object status = payload.get("orderStatus");
            if (status instanceof String && StringUtils.hasText((String) status)) {
                order.setOrderStatus(((String) status).trim());
            }
            
            // 物流单号
            Object trackingNumber = payload.get("trackingNumber");
            if (trackingNumber instanceof String && StringUtils.hasText((String) trackingNumber)) {
                order.setTrackingNumber(((String) trackingNumber).trim());
            }
            
            // 卖家可见性
            Object sellerVisibility = payload.get("sellerVisibility");
            if (sellerVisibility instanceof String && StringUtils.hasText((String) sellerVisibility)) {
                String vis = ((String) sellerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC", "PRIVATE", "HIDDEN"));
                if (!allowedVis.contains(vis)) {
                    return Result.fail("非法的卖家可见性");
                }
                order.setSellerVisibility(vis);
            }
            
            // 买家可见性
            Object buyerVisibility = payload.get("buyerVisibility");
            if (buyerVisibility instanceof String && StringUtils.hasText((String) buyerVisibility)) {
                String vis = ((String) buyerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC", "PRIVATE", "HIDDEN"));
                if (!allowedVis.contains(vis)) {
                    return Result.fail("非法的买家可见性");
                }
                order.setBuyerVisibility(vis);
            }
            
            orderRepository.save(order);
            return Result.ok("订单更新成功", toSimpleOrder(order));
        } catch (Exception e) {
            log.error("完整更新订单异常: orderId={}, error={}", orderId, e.getMessage());
            return Result.fail("更新订单失败");
        }
    }

    @Override
    public Result deleteOrder(String orderId) {
        try {
            if (!orderRepository.existsById(orderId)) {
                return Result.fail("订单不存在");
            }
            orderRepository.deleteById(orderId);
            return Result.ok("订单删除成功");
        } catch (Exception e) {
            log.error("删除订单异常: orderId={}, error={}", orderId, e.getMessage());
            return Result.fail("删除订单失败");
        }
    }

    // ===================== 管理端最小CRUD：会话/消息 =====================
    @Override
    public Result listConversations(Integer page, Integer size, String keyword) {
        try {
            // 构建查询条件
            org.springframework.data.jpa.domain.Specification<com.njumarket.njumarket.entity.Conversation> spec = (root, query, cb) -> {
                java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
                if (org.springframework.util.StringUtils.hasText(keyword)) {
                    String kw = keyword.trim();
                    predicates.add(cb.or(
                        cb.like(root.get("userId1"), "%" + kw + "%"),
                        cb.like(root.get("userId2"), "%" + kw + "%"),
                        cb.like(root.get("lastMessageContent"), "%" + kw + "%")
                    ));
                }
                return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "lastMessageTime"));
            Page<com.njumarket.njumarket.entity.Conversation> p = conversationRepository.findAll(spec, pageable);
            
            // ✅ 批量查询所有用户的 UserProfile（避免 N+1 查询）
            List<com.njumarket.njumarket.entity.Conversation> conversations = p.getContent();
            Set<String> userIds = new HashSet<>();
            for (com.njumarket.njumarket.entity.Conversation c : conversations) {
                if (c.getUserId1() != null) userIds.add(c.getUserId1());
                if (c.getUserId2() != null) userIds.add(c.getUserId2());
            }
            
            Map<String, com.njumarket.njumarket.entity.UserProfile> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<com.njumarket.njumarket.entity.UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
                profileMap = profiles.stream()
                        .collect(java.util.stream.Collectors.toMap(com.njumarket.njumarket.entity.UserProfile::getUserId, profile -> profile));
            }
            
            // ✅ 转换为包含用户信息的简单对象
            final Map<String, com.njumarket.njumarket.entity.UserProfile> finalProfileMap = profileMap;
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
            Optional<com.njumarket.njumarket.entity.Conversation> opt = conversationRepository.findById(conversationId);
            if (opt.isEmpty()) {
                return Result.fail("会话不存在");
            }
            com.njumarket.njumarket.entity.Conversation c = opt.get();
            
            // ✅ 批量查询用户Profile（避免N+1查询）
            Set<String> userIds = new HashSet<>();
            if (c.getUserId1() != null) userIds.add(c.getUserId1());
            if (c.getUserId2() != null) userIds.add(c.getUserId2());
            
            Map<String, com.njumarket.njumarket.entity.UserProfile> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<com.njumarket.njumarket.entity.UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
                profileMap = profiles.stream()
                        .collect(java.util.stream.Collectors.toMap(com.njumarket.njumarket.entity.UserProfile::getUserId, profile -> profile));
            }
            
            Map<String, Object> result = toSimpleConversationWithUsers(c, 
                    profileMap.get(c.getUserId1()), 
                    profileMap.get(c.getUserId2()));
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取会话详情异常: conversationId={}, error={}", conversationId, e.getMessage());
            return Result.fail("获取会话详情失败");
        }
    }

    @Override
    public Result updateConversationFull(String conversationId, java.util.Map<String, Object> payload) {
        try {
            Optional<com.njumarket.njumarket.entity.Conversation> opt = conversationRepository.findById(conversationId);
            if (opt.isEmpty()) {
                return Result.fail("会话不存在");
            }
            com.njumarket.njumarket.entity.Conversation conversation = opt.get();

            // 状态
            Object status = payload.get("status");
            if (status instanceof String && org.springframework.util.StringUtils.hasText((String) status)) {
                String s = ((String) status).trim();
                java.util.Set<String> allowedStatuses = new java.util.HashSet<>(java.util.Arrays.asList("ACTIVE", "DELETED", "ARCHIVED", "BLOCKED"));
                if (!allowedStatuses.contains(s)) {
                    return Result.fail("非法的会话状态");
                }
                conversation.setStatus(s);
            }

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
                    org.springframework.data.domain.Pageable pageable = 
                            org.springframework.data.domain.PageRequest.of(0, 1);
                    List<com.njumarket.njumarket.entity.Message> lastMessages = 
                            messageRepository.findLastMessageForUser(conversationId, conversation.getUserId1(), pageable);
                    
                    if (!lastMessages.isEmpty()) {
                        com.njumarket.njumarket.entity.Message lastMessage = lastMessages.get(0);
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
                    org.springframework.data.domain.Pageable pageable = 
                            org.springframework.data.domain.PageRequest.of(0, 1);
                    List<com.njumarket.njumarket.entity.Message> lastMessages = 
                            messageRepository.findLastMessageForUser(conversationId, conversation.getUserId2(), pageable);
                    
                    if (!lastMessages.isEmpty()) {
                        com.njumarket.njumarket.entity.Message lastMessage = lastMessages.get(0);
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
            return Result.ok("会话更新成功", toSimpleConversationWithUsers(conversation, null, null));
        } catch (Exception e) {
            log.error("完整更新会话异常: conversationId={}, error={}", conversationId, e.getMessage());
            return Result.fail("更新会话失败");
        }
    }

    @Override
    public Result deleteConversation(String conversationId) {
        try {
            if (!conversationRepository.existsById(conversationId)) {
                return Result.fail("会话不存在");
            }
            conversationRepository.deleteById(conversationId);
            return Result.ok("会话删除成功");
        } catch (Exception e) {
            log.error("删除会话异常: conversationId={}, error={}", conversationId, e.getMessage());
            return Result.fail("删除会话失败");
        }
    }

    @Override
    public Result listMessages(String conversationId, Integer page, Integer size) {
        try {
            // ✅ 管理端：不过滤双方都被删除的消息，显示所有消息（包括双方都删除的）
            // 注意：此处使用 findAll 而非 findByConversationId，因为 findByConversationId 会过滤双方都删除的消息
            org.springframework.data.jpa.domain.Specification<com.njumarket.njumarket.entity.Message> spec = (root, query, cb) -> {
                // 只按 conversationId 过滤，不添加任何删除状态过滤条件
                return cb.equal(root.get("conversationId"), conversationId);
            };
            
            Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<com.njumarket.njumarket.entity.Message> p = messageRepository.findAll(spec, pageable);
            
            // 统计双方都删除的消息数量（用于日志和验证）
            long bothDeletedCount = 0;
            List<Map<String, Object>> simpleList = new ArrayList<>();
            for (com.njumarket.njumarket.entity.Message m : p.getContent()) {
                // 统计双方都删除的消息
                if (Boolean.TRUE.equals(m.getDeletedBySender()) && Boolean.TRUE.equals(m.getDeletedByReceiver())) {
                    bothDeletedCount++;
                }
                simpleList.add(toSimpleMessage(m));
            }
            
            // 记录日志，验证是否查询到了双方都删除的消息
            log.debug("管理端查询消息列表: conversationId={}, page={}, size={}, total={}, bothDeletedCount={}", 
                    conversationId, page, size, p.getTotalElements(), bothDeletedCount);
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", simpleList);
            result.put("total", p.getTotalElements());
            result.put("pages", p.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            // 添加统计信息，方便前端了解有多少双方都删除的消息
            result.put("bothDeletedCount", bothDeletedCount);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取消息列表异常: conversationId={}, error={}", conversationId, e.getMessage());
            return Result.fail("获取消息列表失败");
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

    private Map<String, Object> toSimpleUser(com.njumarket.njumarket.entity.User u) {
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
    private Map<String, Object> toSimpleCommodityWithSeller(com.njumarket.njumarket.entity.Commodity c, com.njumarket.njumarket.entity.UserProfile sellerProfile) {
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
            Optional<com.njumarket.njumarket.entity.Commodity> opt = commodityRepository.findById(commodityId);
            if (opt.isEmpty()) return Result.fail("商品不存在");
            com.njumarket.njumarket.entity.Commodity c = opt.get();
            Object title = payload.get("title"); if (title instanceof String) c.setTitle(((String) title).trim());
            Object description = payload.get("description"); if (description instanceof String) c.setDescription(((String) description).trim());
            // ✅ 价格：支持 Number 和 String 类型
            Object price = payload.get("price");
            if (price != null) {
                try {
                    double priceValue = price instanceof Number 
                        ? ((Number) price).doubleValue() 
                        : Double.parseDouble(price.toString().trim());
                    if (priceValue >= 0) {
                        c.setPrice(priceValue);
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
                        c.setStock(stockValue);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            Object location = payload.get("location"); if (location instanceof String) c.setLocation(((String) location).trim());
            Object category = payload.get("category"); if (category instanceof String) c.setCategory(((String) category).trim());
            Object conditionLevel = payload.get("conditionLevel");
            if (conditionLevel instanceof String) {
                String lvl = ((String) conditionLevel).trim();
                java.util.Set<String> allowedLvl = new java.util.HashSet<>(java.util.Arrays.asList(
                    "全新","九成新","八成新","七成新","六成新","五成新"
                ));
                if (!allowedLvl.contains(lvl)) return Result.fail("非法的成色等级");
                c.setConditionLevel(lvl);
            }
            Object commodityStatus = payload.get("commodityStatus");
            if (commodityStatus instanceof String) {
                String st = ((String) commodityStatus).trim();
                java.util.Set<String> allowedStatus = new java.util.HashSet<>(java.util.Arrays.asList("DRAFT","PUBLISHED","ON_SHELF","OFF_SHELF"));
                if (!allowedStatus.contains(st)) return Result.fail("非法的商品状态");
                c.setCommodityStatus(st);
            }
            Object images = payload.get("images"); if (images instanceof String) c.setImages(((String) images).trim());
            // ✅ 点击量：支持 Number 和 String 类型
            Object clickCount = payload.get("clickCount");
            if (clickCount != null) {
                try {
                    int count = clickCount instanceof Number 
                        ? ((Number) clickCount).intValue() 
                        : Integer.parseInt(clickCount.toString().trim());
                    if (count >= 0) {
                        c.setClickCount(count);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            // 分类校验（示例白名单，可根据业务扩展或改为从数据库/配置读取）
            if (category instanceof String) {
                String cat = ((String) category).trim();
                java.util.Set<String> allowedCat = new java.util.HashSet<>(java.util.Arrays.asList(
                    "电子产品","服装配饰","图书文具","生活用品","运动户外","美妆护肤","其他"
                ));
                if (!allowedCat.contains(cat)) return Result.fail("非法的商品分类");
                c.setCategory(cat);
            }
            // 可见性（允许编辑）
            Object sellerVisibility = payload.get("sellerVisibility");
            if (sellerVisibility instanceof String) {
                String vis = ((String) sellerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC","PRIVATE","HIDDEN"));
                if (!allowedVis.contains(vis)) return Result.fail("非法的卖家可见性");
                c.setSellerVisibility(vis);
            }
            Object buyerVisibility = payload.get("buyerVisibility");
            if (buyerVisibility instanceof String) {
                String vis = ((String) buyerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC","PRIVATE","HIDDEN"));
                if (!allowedVis.contains(vis)) return Result.fail("非法的买家可见性");
                c.setBuyerVisibility(vis);
            }
            // 不允许编辑：publish_time、report_count、seller_id、commodity_id
            commodityRepository.save(c);
            return Result.ok("更新成功", toSimpleCommodity(c));
        } catch (Exception e) {
            log.error("完整更新商品异常: commodityId={}, error={}", commodityId, e.getMessage());
            return Result.fail("更新失败");
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
                                                       com.njumarket.njumarket.entity.UserProfile buyerProfile,
                                                       com.njumarket.njumarket.entity.UserProfile sellerProfile) {
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

    private Map<String, Object> toSimpleConversation(com.njumarket.njumarket.entity.Conversation c) {
        return toSimpleConversationWithUsers(c, null, null);
    }
    
    /**
     * 将会话实体转换为简单Map（包含用户信息）
     * @param c 会话实体
     * @param user1Profile 用户1的Profile（可选，如果为null则不在结果中包含用户信息）
     * @param user2Profile 用户2的Profile（可选，如果为null则不在结果中包含用户信息）
     * @return 简单Map对象
     */
    private Map<String, Object> toSimpleConversationWithUsers(com.njumarket.njumarket.entity.Conversation c,
                                                               com.njumarket.njumarket.entity.UserProfile user1Profile,
                                                               com.njumarket.njumarket.entity.UserProfile user2Profile) {
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

    private Map<String, Object> toSimpleMessage(com.njumarket.njumarket.entity.Message m0) {
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
            Optional<com.njumarket.njumarket.entity.Message> opt = messageRepository.findById(messageId);
            if (opt.isEmpty()) {
                return Result.fail("消息不存在");
            }
            return Result.ok(toSimpleMessage(opt.get()));
        } catch (Exception e) {
            log.error("获取消息详情异常: messageId={}, error={}", messageId, e.getMessage());
            return Result.fail("获取消息详情失败");
        }
    }

    @Override
    public Result updateMessageFull(String messageId, java.util.Map<String, Object> payload) {
        try {
            Optional<com.njumarket.njumarket.entity.Message> opt = messageRepository.findById(messageId);
            if (opt.isEmpty()) {
                return Result.fail("消息不存在");
            }
            com.njumarket.njumarket.entity.Message message = opt.get();
            String conversationId = message.getConversationId();
            
            // ✅ 保存原来的可见性状态（用于检测变化）
            Boolean oldDeletedBySender = message.getDeletedBySender();
            Boolean oldDeletedByReceiver = message.getDeletedByReceiver();
            
            // ✅ 获取对话信息（用于更新用户级别的最后消息字段）
            Optional<com.njumarket.njumarket.entity.Conversation> convOpt = 
                    conversationRepository.findById(conversationId);

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

            // ✅ 保存消息的更改
            messageRepository.save(message);
            
            // ✅ 如果可见性发生变化，需要更新相关用户的最后消息字段
            if (convOpt.isPresent() && (newDeletedBySender != null || newDeletedByReceiver != null)) {
                com.njumarket.njumarket.entity.Conversation conversation = convOpt.get();
                String senderId = message.getSenderId();
                String receiverId = message.getReceiverId();
                
                // 检查发送方可见性的变化
                if (newDeletedBySender != null && !newDeletedBySender.equals(oldDeletedBySender)) {
                    // 发送方可见性发生了变化
                    boolean wasVisible = !Boolean.TRUE.equals(oldDeletedBySender);
                    boolean isNowVisible = !Boolean.TRUE.equals(newDeletedBySender);
                    
                    if (wasVisible && !isNowVisible) {
                        // 从可见变为不可见（标记删除）
                        // 检查是否是发送方的最后一条可见消息
                        String senderLastContent = conversation.getLastMessageContentForUser(senderId);
                        LocalDateTime senderLastTime = conversation.getLastMessageTimeForUser(senderId);
                        boolean isSenderLastMessage = senderLastContent != null && senderLastTime != null &&
                            message.getContent().equals(senderLastContent) && 
                            message.getCreatedAt().equals(senderLastTime);
                        
                        if (isSenderLastMessage) {
                            // 查询发送方可见的倒数第二条消息
                            try {
                                org.springframework.data.domain.Pageable pageable = 
                                        org.springframework.data.domain.PageRequest.of(0, 1);
                                List<com.njumarket.njumarket.entity.Message> lastMessages = 
                                        messageRepository.findLastMessageForUser(conversationId, senderId, pageable);
                                
                                if (!lastMessages.isEmpty()) {
                                    com.njumarket.njumarket.entity.Message newLastMessage = lastMessages.get(0);
                                    conversation.setLastMessageForUser(senderId, 
                                            newLastMessage.getContent(), 
                                            newLastMessage.getCreatedAt());
                                } else {
                                    conversation.setLastMessageForUser(senderId, null, null);
                                }
                            } catch (Exception e) {
                                log.warn("管理端更新消息可见性时更新发送方最后消息失败: conversationId={}, messageId={}, error={}", 
                                        conversationId, messageId, e.getMessage());
                            }
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
                        }
                    }
                }
                
                // 检查接收方可见性的变化
                if (newDeletedByReceiver != null && !newDeletedByReceiver.equals(oldDeletedByReceiver)) {
                    // 接收方可见性发生了变化
                    boolean wasVisible = !Boolean.TRUE.equals(oldDeletedByReceiver);
                    boolean isNowVisible = !Boolean.TRUE.equals(newDeletedByReceiver);
                    
                    if (wasVisible && !isNowVisible) {
                        // 从可见变为不可见（标记删除）
                        // 检查是否是接收方的最后一条可见消息
                        String receiverLastContent = conversation.getLastMessageContentForUser(receiverId);
                        LocalDateTime receiverLastTime = conversation.getLastMessageTimeForUser(receiverId);
                        boolean isReceiverLastMessage = receiverLastContent != null && receiverLastTime != null &&
                            message.getContent().equals(receiverLastContent) && 
                            message.getCreatedAt().equals(receiverLastTime);
                        
                        if (isReceiverLastMessage) {
                            // 查询接收方可见的倒数第二条消息
                            try {
                                org.springframework.data.domain.Pageable pageable = 
                                        org.springframework.data.domain.PageRequest.of(0, 1);
                                List<com.njumarket.njumarket.entity.Message> lastMessages = 
                                        messageRepository.findLastMessageForUser(conversationId, receiverId, pageable);
                                
                                if (!lastMessages.isEmpty()) {
                                    com.njumarket.njumarket.entity.Message newLastMessage = lastMessages.get(0);
                                    conversation.setLastMessageForUser(receiverId, 
                                            newLastMessage.getContent(), 
                                            newLastMessage.getCreatedAt());
                                } else {
                                    conversation.setLastMessageForUser(receiverId, null, null);
                                }
                            } catch (Exception e) {
                                log.warn("管理端更新消息可见性时更新接收方最后消息失败: conversationId={}, messageId={}, error={}", 
                                        conversationId, messageId, e.getMessage());
                            }
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
                        }
                    }
                }
                
                // 保存对话的更新
                conversationRepository.save(conversation);
            }
            
            return Result.ok("消息更新成功", toSimpleMessage(message));
        } catch (Exception e) {
            log.error("完整更新消息异常: messageId={}, error={}", messageId, e.getMessage());
            return Result.fail("更新消息失败");
        }
    }

    @Override
    public Result deleteMessage(String messageId) {
        try {
            Optional<com.njumarket.njumarket.entity.Message> msgOpt = messageRepository.findById(messageId);
            if (msgOpt.isEmpty()) {
                return Result.fail("消息不存在");
            }
            
            com.njumarket.njumarket.entity.Message message = msgOpt.get();
            String conversationId = message.getConversationId();
            
            // ✅ 管理端硬删除消息前，检查并更新相关用户的最后消息字段
            Optional<com.njumarket.njumarket.entity.Conversation> convOpt = 
                    conversationRepository.findById(conversationId);
            if (convOpt.isPresent()) {
                com.njumarket.njumarket.entity.Conversation conversation = convOpt.get();
                
                // ✅ 检查消息对用户1是否可见，以及是否是最后一条可见消息
                String user1Id = conversation.getUserId1();
                boolean isVisibleToUser1 = false;
                if (user1Id.equals(message.getSenderId())) {
                    // 用户1是发送方，检查是否被发送方删除
                    isVisibleToUser1 = !Boolean.TRUE.equals(message.getDeletedBySender());
                } else if (user1Id.equals(message.getReceiverId())) {
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
                if (user2Id.equals(message.getSenderId())) {
                    // 用户2是发送方，检查是否被发送方删除
                    isVisibleToUser2 = !Boolean.TRUE.equals(message.getDeletedBySender());
                } else if (user2Id.equals(message.getReceiverId())) {
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
                        org.springframework.data.domain.Pageable pageable = 
                                org.springframework.data.domain.PageRequest.of(0, 2);
                        List<com.njumarket.njumarket.entity.Message> lastMessages = 
                                messageRepository.findLastMessageForUser(
                                        conversationId, conversation.getUserId1(), pageable);
                        
                        if (lastMessages.size() > 1) {
                            // 有第二条消息，使用第二条作为新的最后消息
                            com.njumarket.njumarket.entity.Message newLastMessage = lastMessages.get(1);
                            conversation.setUser1LastMessageContent(newLastMessage.getContent());
                            conversation.setUser1LastMessageTime(newLastMessage.getCreatedAt());
                        } else {
                            // 没有其他可见消息了，设置为空
                            conversation.setUser1LastMessageContent(null);
                            conversation.setUser1LastMessageTime(null);
                        }
                    } catch (Exception e) {
                        log.warn("管理端删除消息时更新用户1最后消息失败: conversationId={}, messageId={}, error={}", 
                                conversationId, messageId, e.getMessage());
                    }
                }
                
                if (isUser2LastMessage) {
                    try {
                        // 查询用户2可见的前2条消息，第一条是要删除的，取第二条
                        org.springframework.data.domain.Pageable pageable = 
                                org.springframework.data.domain.PageRequest.of(0, 2);
                        List<com.njumarket.njumarket.entity.Message> lastMessages = 
                                messageRepository.findLastMessageForUser(
                                        conversationId, conversation.getUserId2(), pageable);
                        
                        if (lastMessages.size() > 1) {
                            // 有第二条消息，使用第二条作为新的最后消息
                            com.njumarket.njumarket.entity.Message newLastMessage = lastMessages.get(1);
                            conversation.setUser2LastMessageContent(newLastMessage.getContent());
                            conversation.setUser2LastMessageTime(newLastMessage.getCreatedAt());
                        } else {
                            // 没有其他可见消息了，设置为空
                            conversation.setUser2LastMessageContent(null);
                            conversation.setUser2LastMessageTime(null);
                        }
                    } catch (Exception e) {
                        log.warn("管理端删除消息时更新用户2最后消息失败: conversationId={}, messageId={}, error={}", 
                                conversationId, messageId, e.getMessage());
                    }
                }
                
                // 保存更新后的对话（如果有任何字段被更新）
                if (isUser1LastMessage || isUser2LastMessage) {
                    conversationRepository.save(conversation);
                }
            }
            
            // 执行硬删除
            messageRepository.deleteById(messageId);
            return Result.ok("消息删除成功");
        } catch (Exception e) {
            log.error("删除消息异常: messageId={}, error={}", messageId, e.getMessage());
            return Result.fail("删除消息失败");
        }
    }
}