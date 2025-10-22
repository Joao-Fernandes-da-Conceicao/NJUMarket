package com.njumarket.njumarket.controller.admin;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.AdminLoginDTO;
import com.njumarket.njumarket.entity.Admin;
import com.njumarket.njumarket.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员控制器
 */
@Tag(name = "管理员管理", description = "管理员登录和管理相关接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "管理员登录", description = "管理员使用用户名和密码登录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public Result login(@RequestBody AdminLoginDTO loginDTO, HttpSession session) {
        return adminService.login(loginDTO, session);
    }

    @Operation(summary = "管理员登出", description = "管理员登出系统")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登出成功")
    })
    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        return adminService.logout(session);
    }

    @Operation(summary = "获取当前管理员信息", description = "获取当前登录管理员的详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "管理员未登录")
    })
    @GetMapping("/me")
    public Result getCurrentAdmin() {
        return adminService.getCurrentAdmin();
    }

    @Operation(summary = "创建管理员", description = "创建新的管理员账号")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "409", description = "用户名已存在")
    })
    @PostMapping("/create")
    public Result createAdmin(@RequestBody Admin admin) {
        return adminService.createAdmin(admin);
    }

    @Operation(summary = "更新管理员信息", description = "更新管理员的基本信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @PutMapping("/{adminId}")
    public Result updateAdmin(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @RequestBody Admin admin) {
        return adminService.updateAdmin(adminId, admin);
    }

    @Operation(summary = "删除管理员", description = "删除指定的管理员账号")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在"),
        @ApiResponse(responseCode = "403", description = "不能删除超级管理员")
    })
    @DeleteMapping("/{adminId}")
    public Result deleteAdmin(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId) {
        return adminService.deleteAdmin(adminId);
    }

    @Operation(summary = "获取管理员列表", description = "分页获取管理员列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/list")
    public Result getAdminList(
        @Parameter(description = "页码，从0开始")
        @RequestParam(defaultValue = "0") Integer page,
        @Parameter(description = "每页大小")
        @RequestParam(defaultValue = "10") Integer size,
        @Parameter(description = "搜索关键词")
        @RequestParam(required = false) String keyword) {
        return adminService.getAdminList(page, size, keyword);
    }

    @Operation(summary = "获取管理员详情", description = "根据ID获取管理员详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @GetMapping("/{adminId}")
    public Result getAdminById(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId) {
        return adminService.getAdminById(adminId);
    }

    @Operation(summary = "更新管理员状态", description = "更新管理员的账户状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @PutMapping("/{adminId}/status")
    public Result updateAdminStatus(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @Parameter(description = "账户状态", required = true)
        @RequestParam String status) {
        return adminService.updateAdminStatus(adminId, status);
    }

    @Operation(summary = "重置管理员密码", description = "重置指定管理员的密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重置成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @PutMapping("/{adminId}/reset-password")
    public Result resetPassword(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @Parameter(description = "新密码", required = true)
        @RequestParam String newPassword) {
        return adminService.resetPassword(adminId, newPassword);
    }

    @Operation(summary = "修改密码", description = "管理员修改自己的密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "400", description = "原密码错误")
    })
    @PutMapping("/change-password")
    public Result changePassword(
        @Parameter(description = "管理员ID", required = true)
        @RequestParam String adminId,
        @Parameter(description = "原密码", required = true)
        @RequestParam String oldPassword,
        @Parameter(description = "新密码", required = true)
        @RequestParam String newPassword) {
        return adminService.changePassword(adminId, oldPassword, newPassword);
    }

    @Operation(summary = "获取管理员统计信息", description = "获取管理员的统计信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/statistics")
    public Result getAdminStatistics() {
        return adminService.getAdminStatistics();
    }

    @Operation(summary = "检查管理员权限", description = "检查管理员是否具有指定权限")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "检查成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @GetMapping("/{adminId}/permission")
    public Result checkPermission(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @Parameter(description = "权限名称", required = true)
        @RequestParam String permission) {
        return adminService.checkPermission(adminId, permission);
    }

    @Operation(summary = "更新管理员权限", description = "更新管理员的权限列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "管理员不存在")
    })
    @PutMapping("/{adminId}/permissions")
    public Result updatePermissions(
        @Parameter(description = "管理员ID", required = true)
        @PathVariable String adminId,
        @RequestBody List<String> permissions) {
        return adminService.updatePermissions(adminId, permissions);
    }
}
