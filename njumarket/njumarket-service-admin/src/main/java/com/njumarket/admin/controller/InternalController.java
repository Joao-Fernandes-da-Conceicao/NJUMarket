package com.njumarket.admin.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.entity.Admin;
import com.njumarket.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 内部API控制器
 * 用于微服务间调用，不对外暴露
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    
    private final AdminRepository adminRepository;
    
    /**
     * 根据ID查询管理员（内部接口）
     */
    @GetMapping("/admin/{adminId}")
    public Result getAdminById(@PathVariable String adminId) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.fail("管理员不存在");
            }
            return Result.ok("查询成功", adminOpt.get());
        } catch (Exception e) {
            log.error("查询管理员失败: adminId={}, error={}", adminId, e.getMessage(), e);
            return Result.fail("查询管理员失败");
        }
    }
    
    /**
     * 检查管理员是否存在（内部接口）
     */
    @GetMapping("/admin/{adminId}/exists")
    public Result adminExists(@PathVariable String adminId) {
        try {
            boolean exists = adminRepository.existsById(adminId);
            return Result.ok("查询成功", exists);
        } catch (Exception e) {
            log.error("检查管理员是否存在失败: adminId={}, error={}", adminId, e.getMessage(), e);
            return Result.fail("检查失败");
        }
    }
}

