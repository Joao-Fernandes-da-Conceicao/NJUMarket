package com.njumarket.auth.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.UserDTO;
import com.njumarket.njumarket.dto.LoginFormDTO;
import com.njumarket.njumarket.dto.PasswordDTO;
import com.njumarket.njumarket.dto.RegisterDTO;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

/**
 * 用户认证控制器
 */
@Tag(name = "用户认证", description = "用户登录、注册、验证码等认证相关接口")
@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserService userService;

    @Operation(summary = "用户登录", description = "使用手机号和密码进行登录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "登录失败")
    })
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        return userService.login(loginForm, session);
    }

    @Operation(summary = "用户注册", description = "注册新用户账号")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功"),
        @ApiResponse(responseCode = "400", description = "注册失败")
    })
    @PostMapping("/register")
    public Result register(@RequestBody UserDTO userDTO) {
        return userService.register(userDTO);
    }

    @Operation(summary = "用户注册（新版）", description = "使用手机号和密码注册新用户（无需验证码，适用于安全要求较低的环境）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功，返回用户信息和Token"),
        @ApiResponse(responseCode = "400", description = "注册失败，参数错误或手机号已存在")
    })
    @PostMapping("/register-new")
    public Result registerNew(@RequestBody RegisterDTO registerDTO) {
        return userService.registerUser(registerDTO);
    }

    @Operation(summary = "发送验证码", description = "向指定手机号发送登录验证码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "验证码发送成功"),
        @ApiResponse(responseCode = "400", description = "手机号格式错误")
    })
    @PostMapping("/send-code")
    public Result sendCode(@Parameter(description = "手机号", example = "13800138000") @RequestParam String phone) {
        return userService.sendCode(phone);
    }

    @Operation(summary = "验证码登录", description = "使用手机号和验证码进行登录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "验证码错误或已过期")
    })
    @PostMapping("/login-by-code")
    public Result loginByCode(@Parameter(description = "手机号", example = "13800138000") @RequestParam String phone, 
                             @Parameter(description = "验证码", example = "123456") @RequestParam String code,
                             HttpSession session) {
        return userService.loginByCode(phone, code, session);
    }

    /**
     * 第三方登录
     */
    @PostMapping("/login-third-party")
    public Result loginThirdParty(@RequestParam String type,
                                 @RequestParam String code,
                                 HttpSession session) {
        return userService.loginThirdParty(type, code, session);
    }

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的基本信息和档案信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/me")
    public Result getCurrentUser() {
        return userService.getCurrentUser();
    }
    
    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        return userService.logout(session);
    }
    
    @Operation(summary = "刷新Token", description = "使用RefreshToken刷新AccessToken")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "刷新成功，返回新的AccessToken和RefreshToken"),
        @ApiResponse(responseCode = "400", description = "RefreshToken无效或已过期")
    })
    @PostMapping("/refresh-token")
    public Result refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BusinessException("RefreshToken不能为空");
        }
        return userService.refreshToken(refreshToken.trim());
    }

    @Operation(summary = "重置密码", description = "通过手机验证码重置密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "密码重置成功"),
        @ApiResponse(responseCode = "400", description = "验证码错误或密码格式不正确")
    })
    @PostMapping("/reset-password")
    public Result resetPassword(@RequestBody PasswordDTO passwordDTO) {
        // 验证密码确认
        if (passwordDTO.getConfirmPassword() != null && 
            !passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        
        return userService.resetPassword(passwordDTO.getPhone(), passwordDTO.getCode(), passwordDTO.getNewPassword());
    }
}

