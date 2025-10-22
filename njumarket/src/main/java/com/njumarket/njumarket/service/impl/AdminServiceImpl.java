package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.AdminLoginDTO;
import com.njumarket.njumarket.entity.Admin;
import com.njumarket.njumarket.repository.AdminRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
                return Result.fail("账户已被禁用，请联系超级管理员");
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
            // 1. 参数验证
            if (!StringUtils.hasText(admin.getUsername())) {
                return Result.fail("用户名不能为空");
            }
            if (!StringUtils.hasText(admin.getPassword())) {
                return Result.fail("密码不能为空");
            }

            // 2. 检查用户名是否已存在
            if (adminRepository.existsByUsername(admin.getUsername())) {
                return Result.fail("用户名已存在");
            }

            // 3. 设置默认值
            if (admin.getAdminId() == null) {
                admin.setAdminId("ADMIN_" + System.currentTimeMillis());
            }
            admin.setPassword(passwordService.encodePassword(admin.getPassword()));
            admin.setAccountStatus("ACTIVE");
            admin.setCreateTime(LocalDateTime.now());
            admin.setUpdateTime(LocalDateTime.now());

            // 4. 保存管理员
            Admin savedAdmin = adminRepository.save(admin);

            log.info("创建管理员成功: adminId={}, username={}", savedAdmin.getAdminId(), savedAdmin.getUsername());

            return Result.ok(savedAdmin);

        } catch (Exception e) {
            log.error("创建管理员异常: username={}, error={}", admin.getUsername(), e.getMessage());
            return Result.fail("创建管理员失败");
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
    public Result getAdminList(Integer page, Integer size, String keyword) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            Page<Admin> adminPage;

            if (StringUtils.hasText(keyword)) {
                // 这里可以添加更复杂的搜索逻辑
                adminPage = adminRepository.findAll(pageable);
            } else {
                adminPage = adminRepository.findAll(pageable);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("list", adminPage.getContent());
            result.put("total", adminPage.getTotalElements());
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", adminPage.getTotalPages());

            return Result.ok(result);

        } catch (Exception e) {
            log.error("获取管理员列表异常: error={}", e.getMessage());
            return Result.fail("获取管理员列表失败");
        }
    }

    @Override
    public Result getAdminById(String adminId) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }

            Admin admin = adminOpt.get();
            // 不返回密码
            admin.setPassword(null);

            return Result.ok(admin);

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

}