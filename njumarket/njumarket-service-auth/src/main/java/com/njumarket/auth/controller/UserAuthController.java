package com.njumarket.auth.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.auth.dto.UserDTO;
import com.njumarket.auth.dto.LoginFormDTO;
import com.njumarket.auth.dto.PasswordDTO;
import com.njumarket.auth.dto.RegisterDTO;
import com.njumarket.auth.dto.UpdatePhoneDTO;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.auth.service.UserService;
import com.njumarket.auth.service.UserAuthCookieService;
import com.njumarket.njumarket.web.AuthCookieNames;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final UserAuthCookieService userAuthCookieService;

    @Operation(summary = "用户登录", description = "使用手机号和密码进行登录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "登录失败")
    })
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginFormDTO loginForm, HttpSession session, HttpServletResponse response) {
        Result r = userService.login(loginForm, session);
        userAuthCookieService.applyLoginCookiesIfSuccess(response, r);
        return r;
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
    public Result registerNew(@Valid @RequestBody RegisterDTO registerDTO, HttpServletResponse response) {
        Result r = userService.registerUser(registerDTO);
        userAuthCookieService.applyLoginCookiesIfSuccess(response, r);
        return r;
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
                             HttpSession session, HttpServletResponse response) {
        Result r = userService.loginByCode(phone, code, session);
        userAuthCookieService.applyLoginCookiesIfSuccess(response, r);
        return r;
    }

    /**
     * 第三方登录
     */
    @PostMapping("/login-third-party")
    public Result loginThirdParty(@RequestParam String type,
                                 @RequestParam String code,
                                 HttpSession session) {
        throw new BusinessException("第三方登录功能未实现");
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
    public Result logout(HttpSession session, HttpServletResponse response) {
        Result r = userService.logout(session);
        if (Boolean.TRUE.equals(r.getSuccess())) {
            userAuthCookieService.clear(response);
        }
        return r;
    }

    @Operation(summary = "刷新Token", description = "使用 RefreshToken（HttpOnly Cookie 或 body）刷新 AccessToken")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "刷新成功"),
        @ApiResponse(responseCode = "400", description = "RefreshToken无效或已过期")
    })
    @PostMapping("/refresh-token")
    public Result refreshToken(HttpServletRequest request, HttpServletResponse response,
                               @RequestBody(required = false) Map<String, String> body) {
        String refreshToken = resolveRefreshToken(request, body);
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException("RefreshToken不能为空");
        }
        Result r = userService.refreshToken(refreshToken.trim());
        userAuthCookieService.applyRefreshCookiesIfSuccess(response, r);
        return r;
    }

    private String resolveRefreshToken(HttpServletRequest request, Map<String, String> body) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (AuthCookieNames.USER_REFRESH.equals(c.getName()) && StringUtils.hasText(c.getValue())) {
                    return c.getValue();
                }
            }
        }
        if (body != null && StringUtils.hasText(body.get("refreshToken"))) {
            return body.get("refreshToken");
        }
        return null;
    }

    @Operation(summary = "重置密码", description = "通过手机验证码重置密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "密码重置成功"),
        @ApiResponse(responseCode = "400", description = "验证码错误或密码格式不正确")
    })
    @PostMapping("/reset-password")
    public Result resetPassword(@Valid @RequestBody PasswordDTO passwordDTO) {
        if (passwordDTO.getConfirmPassword() != null &&
            !passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        return userService.resetPassword(passwordDTO.getPhone(), passwordDTO.getCode(), passwordDTO.getNewPassword());
    }

    @Operation(summary = "修改手机号", description = "用户修改自己的手机号（需要新手机号的验证码）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "手机号修改成功"),
        @ApiResponse(responseCode = "400", description = "验证码错误或手机号已被使用"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @PostMapping("/update-phone")
    public Result updatePhone(@Valid @RequestBody UpdatePhoneDTO updatePhoneDTO) {
        return userService.updatePhone(updatePhoneDTO.getNewPhone(), updatePhoneDTO.getCode());
    }
}
