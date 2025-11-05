package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.UserDTO;
import com.njumarket.njumarket.dto.LoginFormDTO;
import com.njumarket.njumarket.dto.RegisterDTO;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.UserProfile;
import com.njumarket.njumarket.repository.UserRepository;
import com.njumarket.njumarket.service.UserService;
import com.njumarket.njumarket.service.PasswordService;
import com.njumarket.njumarket.service.UserProfileService;
import com.njumarket.njumarket.utils.UserHolder;
import com.njumarket.njumarket.utils.JwtUtils;
import com.njumarket.njumarket.utils.RedisConstants;
import com.njumarket.njumarket.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserProfileService userProfileService;

    // ========== 认证相关 ==========
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. 获取登录标识符（用户名或手机号）和密码
        String identifier = loginForm.getIdentifier();
        String password = loginForm.getPassword();
        
        // 2. 校验输入参数
        if (identifier == null || identifier.trim().isEmpty()) {
            return Result.fail("用户名或手机号不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.fail("密码不能为空");
        }
        
        // 3. 根据标识符查询用户（支持用户名或手机号登录）
        User user = userRepository.findByUsernameOrPhone(identifier.trim()).orElse(null);
        if (user == null) {
            return Result.fail("用户名或手机号不存在");
        }
        
        // 4. 检查账户状态
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            String statusMessage = getAccountStatusMessage(user.getAccountStatus());
            return Result.fail(statusMessage);
        }
        
        // 5. 验证密码
        if (user.getPassword() == null) {
            return Result.fail("该账户未设置密码，请使用手机验证码登录");
        }
        
        if (!passwordService.matches(password, user.getPassword())) {
            log.warn("用户密码验证失败: identifier={}", identifier);
            return Result.fail("密码错误");
        }
        
        // 6. 生成并存储Token
        Map<String, Object> tokenResult = generateAndStoreTokens(user);
        
        // 7. 返回登录结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenResult.get("accessToken"));
        result.put("refreshToken", tokenResult.get("refreshToken"));
        result.put("userInfo", convertToUserDTO(user));
        
        log.info("用户密码登录成功: userId={}, identifier={}", user.getUserId(), identifier);
        return Result.ok(result);
    }

    @Override
    public Result register(UserDTO userDTO) {
        // 保留原有方法，兼容性考虑
        return Result.ok("请使用新的注册接口");
    }

    @Override
    public Result registerUser(RegisterDTO registerDTO) {
        // 1. 校验输入参数
        if (registerDTO.getPhone() == null || registerDTO.getPhone().trim().isEmpty()) {
            return Result.fail("手机号不能为空");
        }
        if (registerDTO.getPassword() == null || registerDTO.getPassword().trim().isEmpty()) {
            return Result.fail("密码不能为空");
        }
        // 注意：验证码已移除，不再需要验证码
        
        // 2. 校验手机号格式
        String phone = registerDTO.getPhone().trim();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        
        // 3. 校验密码强度
        String password = registerDTO.getPassword().trim();
        if (password.length() < 6) {
            return Result.fail("密码长度不能少于6位");
        }
        
        // 4. 校验密码确认
        if (registerDTO.getConfirmPassword() != null && 
            !password.equals(registerDTO.getConfirmPassword().trim())) {
            return Result.fail("两次输入的密码不一致");
        }
        
        // 5. 检查手机号是否已注册
        if (userRepository.existsByPrimaryPhone(phone)) {
            return Result.fail("该手机号已注册");
        }
        
        // 6. 检查用户名是否已存在（如果提供了用户名）
        String username = registerDTO.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            username = username.trim();
            if (userRepository.existsByUsername(username)) {
                return Result.fail("用户名已存在");
            }
            // 校验用户名格式
            if (username.length() < 3 || username.length() > 20) {
                return Result.fail("用户名长度应在3-20位之间");
            }
            if (!username.matches("^[a-zA-Z0-9_]+$")) {
                return Result.fail("用户名只能包含字母、数字和下划线");
            }
        }
        
        // 7. 创建新用户
        User newUser = new User();
        newUser.setUserId(generateUserId());
        newUser.setPrimaryPhone(phone);
        newUser.setUsername(username);
        newUser.setAccountStatus("ACTIVE");
        
        // 8. 加密密码
        String encodedPassword = passwordService.encodePassword(password);
        newUser.setPassword(encodedPassword);
        
        // 9. 保存用户到数据库
        try {
            User savedUser = userRepository.save(newUser);
            
            // 10. 创建用户档案
            createUserProfile(savedUser, registerDTO.getNickname());
            
            // 11. 生成并存储Token（自动登录）
            Map<String, Object> tokenResult = generateAndStoreTokens(savedUser);
            
            // 12. 返回注册结果
            Map<String, Object> result = new HashMap<>();
            result.put("token", tokenResult.get("accessToken"));
            result.put("refreshToken", tokenResult.get("refreshToken"));
            result.put("userInfo", convertToUserDTO(savedUser));
            
            log.info("用户注册成功: userId={}, phone={}, username={}", 
                savedUser.getUserId(), phone, username);
            return Result.ok(result);
            
        } catch (Exception e) {
            log.error("用户注册失败: phone={}, error={}", phone, e.getMessage());
            return Result.fail("注册失败，请稍后重试");
        }
    }

    @Override
    public Result sendCode(String phone) {
        // 1. 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        
        // 2. 生成6位数验证码
        String code = generateVerificationCode();
        
        // 3. 将验证码存储到Redis，5分钟过期
        String codeKey = RedisConstants.LOGIN_CODE_KEY + phone;
        stringRedisTemplate.opsForValue().set(codeKey, code, 
            RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        
        // 4. 控制台输出验证码（模拟短信发送）
        log.info("=== 验证码发送 ===");
        log.info("手机号: {}", phone);
        log.info("验证码: {}", code);
        log.info("有效期: {}分钟", RedisConstants.LOGIN_CODE_TTL);
        log.info("==================");
        
        return Result.ok("验证码发送成功");
    }

    @Override
    public Result loginByCode(String phone, String code, HttpSession session) {
        // 1. 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        
        // 2. 校验验证码格式
        if (RegexUtils.isCodeInvalid(code)) {
            return Result.fail("验证码格式错误");
        }
        
        // 3. 从Redis获取验证码
        String codeKey = RedisConstants.LOGIN_CODE_KEY + phone;
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        
        if (cachedCode == null) {
            return Result.fail("验证码已过期");
        }
        
        if (!cachedCode.equals(code)) {
            return Result.fail("验证码错误");
        }
        
        // 4. 验证码正确，删除Redis中的验证码
        stringRedisTemplate.delete(codeKey);
        
        // 5. 查询或创建用户
        User user = userRepository.findByPrimaryPhone(phone).orElse(null);
        if (user == null) {
            // 用户不存在，自动注册
            user = createUserByPhone(phone);
        }
        
        // 6. 检查账户状态
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            String statusMessage = getAccountStatusMessage(user.getAccountStatus());
            return Result.fail(statusMessage);
        }
        
        // 7. 生成JWT Token
        String token = jwtUtils.generateToken(user.getUserId(), user.getPrimaryPhone());
        String refreshToken = jwtUtils.generateRefreshToken(user.getUserId());
        
        // 8. 将Token存储到Redis
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + user.getUserId();
        stringRedisTemplate.opsForValue().set(tokenKey, token, 
            RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        
        // 9. 返回登录结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("userInfo", convertToUserDTO(user));
        
        log.info("用户验证码登录成功: userId={}, phone={}", user.getUserId(), phone);
        return Result.ok(result);
    }

    @Override
    public Result loginThirdParty(String type, String code, HttpSession session) {
        // TODO:实现第三方登录逻辑
        return Result.ok("登录成功");
    }

    @Override
    public Result logout(HttpSession session) {
        // 1. 获取当前用户
        User currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("用户未登录");
        }
        
        // 2. 删除Redis中的Token
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + currentUser.getUserId();
        stringRedisTemplate.delete(tokenKey);
        
        // 3. 清除ThreadLocal中的用户信息
        UserHolder.removeUser();
        
        log.info("用户登出成功: userId={}", currentUser.getUserId());
        return Result.ok("登出成功");
    }

    @Override
    public Result refreshToken(String refreshToken) {
        // 1. 验证刷新Token
        if (!jwtUtils.validateToken(refreshToken)) {
            return Result.fail("刷新Token无效");
        }
        
        // 2. 检查是否为刷新Token
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            return Result.fail("Token类型错误");
        }
        
        // 3. 从刷新Token中获取用户ID
        String userId = jwtUtils.getUserIdFromToken(refreshToken);
        if (userId == null) {
            return Result.fail("无法获取用户信息");
        }
        
        // 4. 查询用户信息
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        
        // 5. 检查用户状态
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            return Result.fail("账户已被禁用");
        }
        
        // 6. 生成新的Token
        String newToken = jwtUtils.generateToken(user.getUserId(), user.getPrimaryPhone());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getUserId());
        
        // 7. 更新Redis中的Token
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + user.getUserId();
        stringRedisTemplate.opsForValue().set(tokenKey, newToken, 
            RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        
        // 8. 返回新Token
        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("refreshToken", newRefreshToken);
        
        log.info("Token刷新成功: userId={}", userId);
        return Result.ok(result);
    }

    @Override
    public Result resetPassword(String phone, String code, String newPassword) {
        // 1. 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        
        // 2. 校验验证码
        if (code == null || code.trim().isEmpty()) {
            return Result.fail("验证码不能为空");
        }
        
        // 3. 从Redis中获取验证码
        String codeKey = RedisConstants.LOGIN_CODE_KEY + phone;
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            return Result.fail("验证码已过期，请重新获取");
        }
        
        // 4. 验证验证码
        if (!cachedCode.equals(code.trim())) {
            return Result.fail("验证码错误");
        }
        
        // 5. 校验新密码
        if (newPassword == null || newPassword.trim().length() < 6) {
            return Result.fail("密码长度不能少于6位");
        }
        
        // 6. 查找用户
        User user = userRepository.findByPrimaryPhone(phone).orElse(null);
        if (user == null) {
            return Result.fail("该手机号未注册");
        }
        
        // 7. 检查账户状态
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            return Result.fail("账户已被禁用，无法重置密码");
        }
        
        // 8. 加密并设置新密码
        String encodedPassword = passwordService.encodePassword(newPassword.trim());
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        // 9. 删除验证码
        stringRedisTemplate.delete(codeKey);
        
        log.info("用户密码重置成功: phone={}, userId={}", phone, user.getUserId());
        return Result.ok("密码重置成功");
    }

    // ========== 用户档案相关 ==========
    @Override
    public Result getCurrentUser() {
        try {
            // 获取当前用户信息
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 转换为UserDTO（包含profile信息）
            UserDTO userDTO = convertToUserDTO(currentUser);
            return Result.ok(userDTO);
        } catch (Exception e) {
            log.error("获取当前用户信息失败: {}", e.getMessage(), e);
            return Result.fail("获取用户信息失败");
        }
    }

    @Override
    public Result updateProfile(UserDTO userDTO) {
        // 更新用户档案
        return Result.ok("档案更新成功");
    }

    @Override
    public Result uploadAvatar(MultipartFile file) {
        // 上传头像逻辑
        return Result.ok("头像上传成功");
    }

    @Override
    public Result updateContact(String type, String value) {
        // 更新联系方式
        return Result.ok("联系方式更新成功");
    }

    @Override
    public Result getContactList() {
        // 获取联系方式列表
        return Result.ok("获取成功");
    }

    @Override
    public Result deleteContact(String contactId) {
        // 删除联系方式
        return Result.ok("删除成功");
    }

    @Override
    public Result getUserStatistics() {
        // 获取用户统计信息
        return Result.ok("获取成功");
    }

    @Override
    public Result getCreditHistory() {
        // 获取信用记录
        return Result.ok("获取成功");
    }

    @Override
    public Result getUserPublicInfo(String userId) {
        // 获取其他用户公开信息
        return Result.ok("获取成功");
    }

    // ========== 内部方法 ==========
    @Override
    public Result multiLogin(String way, String info) {
        // 多种方式登录实现
        return Result.ok("登录成功");
    }

    @Override
    public Boolean checkPhoneBind(String phone) {
        // 检查手机号绑定
        return userRepository.existsByPrimaryPhone(phone);
    }

    @Override
    public Boolean bindPhoneToUniqueUser(String userId, String phone) {
        // 绑定手机号到用户
        return true;
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 生成6位数验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
    
    /**
     * 根据手机号创建用户
     */
    private User createUserByPhone(String phone) {
        User user = new User();
        user.setUserId(generateUserId());
        user.setPrimaryPhone(phone);
        user.setAccountStatus("ACTIVE");
        user.setRegisterTime(java.time.LocalDateTime.now());
        
        // 保存用户到数据库
        User savedUser = userRepository.save(user);
        
        // 创建用户档案
        createUserProfile(savedUser, null);
        
        return savedUser;
    }
    
    /**
     * 生成用户ID
     */
    private String generateUserId() {
        return "USER_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
    }
    
    /**
     * 转换User实体为UserDTO
     */
    private UserDTO convertToUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setPrimaryPhone(user.getPrimaryPhone());
        userDTO.setAccountStatus(user.getAccountStatus());
        userDTO.setRegisterTime(user.getRegisterTime());
        
        // 从UserProfile获取头像和昵称信息
        if (user.getUserProfile() != null) {
            UserProfile profile = user.getUserProfile();
            userDTO.setNickname(profile.getNickname());
            userDTO.setAvatar(profile.getAvatar());
            userDTO.setCreditScore(profile.getCreditScore());
            userDTO.setBuyerRating(profile.getBuyerRating());
            userDTO.setSellerRating(profile.getSellerRating());
            userDTO.setVipLevel(profile.getVipLevel());
        }
        
        return userDTO;
    }
    
    // ========== 密码管理相关 ==========
    
    /**
     * 设置用户密码
     */
    public Result setPassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            return Result.fail("密码长度不能少于6位");
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        
        // 加密密码
        String encodedPassword = passwordService.encodePassword(newPassword.trim());
        user.setPassword(encodedPassword);
        
        userRepository.save(user);
        log.info("用户密码设置成功: userId={}", userId);
        return Result.ok("密码设置成功");
    }
    
    /**
     * 修改用户密码
     */
    public Result changePassword(String userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Result.fail("原密码不能为空");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            return Result.fail("新密码长度不能少于6位");
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        
        // 验证原密码
        if (user.getPassword() == null) {
            return Result.fail("该账户未设置密码");
        }
        if (!passwordService.matches(oldPassword, user.getPassword())) {
            return Result.fail("原密码错误");
        }
        
        // 检查新密码是否与原密码相同
        if (passwordService.matches(newPassword, user.getPassword())) {
            return Result.fail("新密码不能与原密码相同");
        }
        
        // 更新密码
        String encodedPassword = passwordService.encodePassword(newPassword.trim());
        user.setPassword(encodedPassword);
        
        userRepository.save(user);
        log.info("用户密码修改成功: userId={}", userId);
        return Result.ok("密码修改成功");
    }
    
    /**
     * 重置用户密码（管理员功能）
     */
    public Result resetPassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            return Result.fail("密码长度不能少于6位");
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        
        // 加密密码
        String encodedPassword = passwordService.encodePassword(newPassword.trim());
        user.setPassword(encodedPassword);
        
        userRepository.save(user);
        log.info("管理员重置用户密码: userId={}", userId);
        return Result.ok("密码重置成功");
    }
    
    /**
     * 检查用户是否设置了密码
     */
    public boolean hasPassword(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getPassword() != null && !user.getPassword().trim().isEmpty();
    }
    
    /**
     * 创建用户档案
     */
    private void createUserProfile(User user, String nickname) {
        try {
            Result result = userProfileService.createUserProfile(user.getUserId(), nickname);
            if (result.getSuccess()) {
                log.info("为用户创建档案成功: userId={}, nickname={}", user.getUserId(), nickname);
            } else {
                log.warn("为用户创建档案失败: userId={}, error={}", user.getUserId(), result.getMessage());
            }
        } catch (Exception e) {
            log.warn("创建用户档案异常: userId={}, error={}", user.getUserId(), e.getMessage());
            // 不抛出异常，避免影响注册流程
        }
    }
    
    /**
     * 生成并存储Token（AccessToken和RefreshToken）
     * @param user 用户对象
     * @return 包含accessToken和refreshToken的Map
     */
    private Map<String, Object> generateAndStoreTokens(User user) {
        try {
            // 1. 生成AccessToken和RefreshToken
            String accessToken = jwtUtils.generateToken(user.getUserId(), user.getPrimaryPhone());
            String refreshToken = jwtUtils.generateRefreshToken(user.getUserId());
            
            // 2. 存储AccessToken到Redis
            String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + user.getUserId();
            stringRedisTemplate.opsForValue().set(tokenKey, accessToken, 
                RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
            
            // 3. 存储RefreshToken到Redis
            String refreshKey = RedisConstants.REFRESH_TOKEN_KEY + user.getUserId();
            stringRedisTemplate.opsForValue().set(refreshKey, refreshToken, 
                RedisConstants.REFRESH_TOKEN_TTL, TimeUnit.MINUTES);
            
            // 4. 返回Token信息
            Map<String, Object> tokenResult = new HashMap<>();
            tokenResult.put("accessToken", accessToken);
            tokenResult.put("refreshToken", refreshToken);
            tokenResult.put("expiresIn", RedisConstants.LOGIN_TOKEN_TTL * 60); // 转换为秒
            
            log.debug("Token生成并存储成功: userId={}", user.getUserId());
            return tokenResult;
            
        } catch (Exception e) {
            log.error("Token生成或存储失败: userId={}, error={}", user.getUserId(), e.getMessage());
            throw new RuntimeException("Token生成失败", e);
        }
    }
    
    /**
     * 根据账户状态获取用户友好的提示信息
     */
    private String getAccountStatusMessage(String accountStatus) {
        if (accountStatus == null) {
            return "账户状态异常，请联系管理员";
        }
        
        switch (accountStatus) {
            case "SUSPENDED":
                return "账户已被暂停，请联系管理员了解详情";
            case "BANNED":
                return "账户已被封禁，如有疑问请联系管理员";
            case "DELETED":
                return "账户已被删除，无法使用";
            default:
                return "账户状态异常，请联系管理员";
        }
    }
}
