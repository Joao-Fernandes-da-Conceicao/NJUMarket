package com.njumarket.auth.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.auth.dto.UserDTO;
import com.njumarket.auth.dto.LoginFormDTO;
import com.njumarket.auth.dto.RegisterDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口
 */
public interface UserService {
    
    // ========== 认证相关 ==========
    /**
     * 用户登录
     */
    Result login(LoginFormDTO loginForm, HttpSession session);
    
    /**
     * 用户注册
     */
    Result register(UserDTO userDTO);
    
    /**
     * 用户注册（新版本）
     */
    Result registerUser(RegisterDTO registerDTO);
    
    /**
     * 发送验证码
     */
    Result sendCode(String phone);
    
    /**
     * 验证码登录
     */
    Result loginByCode(String phone, String code, HttpSession session);
    
    /**
     * 第三方登录
     */
    Result loginThirdParty(String type, String code, HttpSession session);
    
    /**
     * 用户登出
     */
    Result logout(HttpSession session);
    
    /**
     * 刷新token
     */
    Result refreshToken(String refreshToken);
    
    /**
     * 重置密码
     */
    Result resetPassword(String phone, String code, String newPassword);
    
    // ========== 用户档案相关 ==========
    /**
     * 获取当前用户信息
     */
    Result getCurrentUser();
    
    /**
     * 更新用户档案
     */
    Result updateProfile(UserDTO userDTO);
    
    /**
     * 上传头像
     */
    Result uploadAvatar(MultipartFile file);
    
    /**
     * 更新联系方式
     */
    Result updateContact(String type, String value);
    
    /**
     * 获取联系方式列表
     */
    Result getContactList();
    
    /**
     * 删除联系方式
     */
    Result deleteContact(String contactId);
    
    /**
     * 获取用户统计信息
     */
    Result getUserStatistics();
    
    /**
     * 获取信用记录
     */
    Result getCreditHistory();
    
    /**
     * 获取其他用户公开信息
     */
    Result getUserPublicInfo(String userId);
    
    // ========== 内部方法 ==========
    /**
     * 多种方式登录
     */
    Result multiLogin(String way, String info);
    
    /**
     * 检查手机号绑定
     */
    Boolean checkPhoneBind(String phone);
    
    /**
     * 绑定手机号到唯一用户
     */
    Boolean bindPhoneToUniqueUser(String userId, String phone);
}

