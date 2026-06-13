package com.njumarket.auth.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.auth.dto.UserDTO;
import com.njumarket.auth.dto.LoginFormDTO;
import com.njumarket.auth.dto.RegisterDTO;
import com.njumarket.auth.dto.internal.UserInternalDTOConverter;
import com.njumarket.auth.entity.UserProfile;
import com.njumarket.auth.repository.UserProfileRepository;
import com.njumarket.auth.vo.LoginResultVO;
import com.njumarket.auth.vo.TokenResultVO;
import com.njumarket.auth.entity.User;
import com.njumarket.auth.repository.UserRepository;
import com.njumarket.auth.service.UserService;
import com.njumarket.auth.service.PasswordService;
import com.njumarket.auth.service.UserProfileService;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.njumarket.utils.JwtUtils;
import com.njumarket.njumarket.utils.RedisConstants;
import com.njumarket.njumarket.utils.RegexUtils;
import com.njumarket.njumarket.utils.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordService passwordService;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserProfileService userProfileService;
    private final CacheUtil cacheUtil;
    private final UserInternalDTOConverter userInternalDTOConverter;

    // ========== 认证相关 ==========
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 参数验证已由@Valid注解自动完成，无需手动验证
        
        // 1. 获取登录标识符（用户名或手机号）和密码
        String identifier = loginForm.getIdentifier();
        String password = loginForm.getPassword();
        
        // 2. 根据标识符查询用户（支持用户名或手机号登录）
        User user = userRepository.findByUsernameOrPhone(identifier.trim())
            .orElseThrow(() -> new BusinessException("用户名或手机号不存在"));
        
        // 4. 检查账户状态
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            String statusMessage = getAccountStatusMessage(user.getAccountStatus());
            throw new BusinessException(statusMessage);
        }
        
        // 5. 验证密码
        if (user.getPassword() == null) {
            throw new BusinessException("该账户未设置密码，请使用手机验证码登录");
        }
        
        if (!passwordService.matches(password, user.getPassword())) {
            log.warn("用户密码验证失败: identifier={}", identifier);
            throw new BusinessException("密码错误");
        }
        
        // 6. 生成并存储Token
        TokenResultVO tokenResult = generateAndStoreTokens(user);
        
        // 7. 返回登录结果
        LoginResultVO result = new LoginResultVO();
        result.setAccessToken(tokenResult.getAccessToken());
        result.setRefreshToken(tokenResult.getRefreshToken());
        result.setExpiresIn(tokenResult.getExpiresIn());
        result.setUserInfo(convertToUserDTO(user));
        
        // ✅ v1.3.x: 添加订单提醒状态（向后兼容，如果字段不存在则返回false）
        try {
            Map<String, Boolean> orderReminderStatus = userProfileService.getOrderReminderStatus(user.getUserId());
            result.setOrderReminderStatus(orderReminderStatus);
        } catch (Exception e) {
            log.debug("获取订单提醒状态失败（可能是旧版本数据库）: userId={}, error={}", user.getUserId(), e.getMessage());
            // 兼容性处理：如果获取失败，返回默认值
            Map<String, Boolean> defaultStatus = new HashMap<>();
            defaultStatus.put("sellerOrderHasNew", false);
            defaultStatus.put("buyerOrderHasNew", false);
            result.setOrderReminderStatus(defaultStatus);
        }
        
        
        return Result.ok(result);
    }

    @Override
    public Result register(UserDTO userDTO) {
        // 保留原有方法，兼容性考虑
        return Result.ok("请使用新的注册接口");
    }

    @Override
    public Result registerUser(RegisterDTO registerDTO) {
        // 参数验证已由@Valid注解自动完成（包括手机号非空、密码非空和长度验证）
        
        // 1. 校验手机号格式
        String phone = registerDTO.getPhone().trim();
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new BusinessException("手机号格式错误");
        }
        
        // 2. 获取密码（长度验证已由@Size注解完成）
        String password = registerDTO.getPassword().trim();
        
        // 3. 校验密码确认
        if (registerDTO.getConfirmPassword() != null && 
            !password.equals(registerDTO.getConfirmPassword().trim())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        
        // 5. 检查手机号是否已注册
        if (userRepository.existsByPrimaryPhone(phone)) {
            throw new BusinessException("该手机号已注册");
        }
        
        // 6. 检查用户名是否已存在（如果提供了用户名）
        String username = registerDTO.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            username = username.trim();
            if (userRepository.existsByUsername(username)) {
                throw new BusinessException("用户名已存在");
            }
            // 校验用户名格式
            if (username.length() < 3 || username.length() > 20) {
                throw new BusinessException("用户名长度应在3-20位之间");
            }
            if (!username.matches("^[a-zA-Z0-9_]+$")) {
                throw new BusinessException("用户名只能包含字母、数字和下划线");
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
        User savedUser = userRepository.save(newUser);
        
        // 10. 创建用户档案
        createUserProfile(savedUser, registerDTO.getNickname());
        
        // 11. 生成并存储Token（自动登录）
        TokenResultVO tokenResult = generateAndStoreTokens(savedUser);
        
        // 12. 返回注册结果
        LoginResultVO result = new LoginResultVO();
        result.setAccessToken(tokenResult.getAccessToken());
        result.setRefreshToken(tokenResult.getRefreshToken());
        result.setExpiresIn(tokenResult.getExpiresIn());
        result.setUserInfo(convertToUserDTO(savedUser));
        
        return Result.ok(result);
    }

    @Override
    public Result sendCode(String phone) {
        // 1. 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new BusinessException("手机号格式错误");
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
            throw new BusinessException("手机号格式错误");
        }
        
        // 2. 校验验证码格式
        if (RegexUtils.isCodeInvalid(code)) {
            throw new BusinessException("验证码格式错误");
        }
        
        // 3. 从Redis获取验证码
        String codeKey = RedisConstants.LOGIN_CODE_KEY + phone;
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        
        if (cachedCode == null) {
            throw new BusinessException("验证码已过期");
        }
        
        if (!cachedCode.equals(code)) {
            throw new BusinessException("验证码错误");
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
            throw new BusinessException(statusMessage);
        }
        
        // 7. 生成并存储Token（AccessToken和RefreshToken）
        TokenResultVO tokenResult = generateAndStoreTokens(user);
        
        // 8. 返回登录结果
        LoginResultVO result = new LoginResultVO();
        result.setAccessToken(tokenResult.getAccessToken());
        result.setRefreshToken(tokenResult.getRefreshToken());
        result.setExpiresIn(tokenResult.getExpiresIn());
        result.setUserInfo(convertToUserDTO(user));
        
        // ✅ v1.3.x: 添加订单提醒状态（向后兼容，如果字段不存在则返回false）
        try {
            Map<String, Boolean> orderReminderStatus = userProfileService.getOrderReminderStatus(user.getUserId());
            result.setOrderReminderStatus(orderReminderStatus);
        } catch (Exception e) {
            log.debug("获取订单提醒状态失败（可能是旧版本数据库）: userId={}, error={}", user.getUserId(), e.getMessage());
            // 兼容性处理：如果获取失败，返回默认值
            Map<String, Boolean> defaultStatus = new HashMap<>();
            defaultStatus.put("sellerOrderHasNew", false);
            defaultStatus.put("buyerOrderHasNew", false);
            result.setOrderReminderStatus(defaultStatus);
        }
        
        
        return Result.ok(result);
    }

    /*
    @Override
    public Result loginThirdParty(String type, String code, HttpSession session) {
        // TODO:实现第三方登录逻辑
        return Result.ok("登录成功");
    }
        */

    @Override
    public Result logout(HttpSession session) {
        // 1. 获取当前用户（SecurityUtils 从 SecurityContext 中取，由 UserContextFilter 设置）
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        String userId = currentUser.getUserId();

        log.info("用户登出开始: userId={}", userId);

        // 2. 通过用户-Session索引找到 accessSessionId，删除对应 Session Hash
        String userSessionIndex = RedisConstants.LOGIN_TOKEN_KEY + userId;
        String accessSessionId = stringRedisTemplate.opsForValue().get(userSessionIndex);
        if (accessSessionId != null) {
            stringRedisTemplate.delete(RedisConstants.SESSION_KEY + accessSessionId);
            log.info("已删除 Access Session: userId={}, accessSessionId={}", userId, accessSessionId);
        }
        // 删除索引本身
        stringRedisTemplate.delete(userSessionIndex);

        // 3. 清除 ThreadLocal / SecurityContext
        SecurityUtils.clearContext();

        log.info("用户登出成功: userId={}", userId);
        return Result.ok("登出成功");
    }

    @Override
    public Result refreshToken(String refreshToken) {
        // 1. 验证 JWT 签名与有效期
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException("刷新Token无效或已过期");
        }
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw new BusinessException("Token类型错误，请使用RefreshToken");
        }

        // 2. 从 JWT 中取出 refreshSessionId
        String refreshSessionId = jwtUtils.getSessionIdFromToken(refreshToken);
        if (refreshSessionId == null) {
            throw new BusinessException("无法从Token中获取Session信息");
        }

        // 3. 查询 Refresh Session，取出 userId
        String refreshSessionKey = RedisConstants.SESSION_REFRESH_KEY + refreshSessionId;
        Object userIdObj = stringRedisTemplate.opsForHash().get(refreshSessionKey, "userId");
        if (userIdObj == null) {
            throw new BusinessException("RefreshToken已失效，请重新登录");
        }
        String userId = userIdObj.toString();

        // 4. 查询用户，并校验状态
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new BusinessException("账户已被禁用");
        }

        // 5. 删除旧 Refresh Session（轮转：每次 refresh 都重新生成，防止泄露复用）
        stringRedisTemplate.delete(refreshSessionKey);

        // 6. 调用 generateAndStoreTokens 生成新 Session + 新 JWT
        TokenResultVO tokenResult = generateAndStoreTokens(user);

        log.info("Token 刷新成功（Session 模式）: userId={}", userId);
        return Result.ok(tokenResult);
    }

    @Override
    public Result resetPassword(String phone, String code, String newPassword) {
        // 参数验证已由@Valid注解自动完成（手机号、验证码、密码非空和长度验证）
        
        // 1. 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new BusinessException("手机号格式错误");
        }
        
        // 2. 从Redis中获取验证码
        String codeKey = RedisConstants.LOGIN_CODE_KEY + phone;
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        
        // 3. 验证验证码
        if (!cachedCode.equals(code.trim())) {
            throw new BusinessException("验证码错误");
        }
        
        // 4. 查找用户
        User user = userRepository.findByPrimaryPhone(phone).orElse(null);
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }
        
        // 5. 检查账户状态
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new BusinessException("账户已被禁用，无法重置密码");
        }
        
        // 6. 加密并设置新密码
        String encodedPassword = passwordService.encodePassword(newPassword.trim());
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        // 7. 删除验证码
        stringRedisTemplate.delete(codeKey);
        
        // ✅ 删除用户信息缓存（密码变更后需要清除缓存）
        if (cacheUtil != null) {
            String cacheKey = RedisConstants.CACHE_USER_INFO_KEY + user.getUserId();
            cacheUtil.delete(cacheKey);
            log.info("已删除用户信息缓存: userId={}, cacheKey={}", user.getUserId(), cacheKey);
        }
        
        log.info("用户密码重置成功: phone={}, userId={}", phone, user.getUserId());
        return Result.ok("密码重置成功");
    }

    @Override
    public Result updatePhone(String newPhone, String code) {
        // 1. 获取当前登录用户
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = userObj instanceof User ? (User) userObj : null;
        if (currentUser == null) {
            throw new BusinessException("用户未登录");
        }
        
        // 2. 校验新手机号格式
        if (RegexUtils.isPhoneInvalid(newPhone)) {
            throw new BusinessException("手机号格式错误");
        }
        
        // 3. 检查新手机号是否已被其他用户使用
        if (userRepository.existsByPrimaryPhone(newPhone)) {
            throw new BusinessException("该手机号已被其他用户使用");
        }
        
        // 4. 从Redis中获取验证码（验证码应该发送到新手机号）
        String codeKey = RedisConstants.LOGIN_CODE_KEY + newPhone;
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        
        // 5. 验证验证码
        if (!cachedCode.equals(code.trim())) {
            throw new BusinessException("验证码错误");
        }
        
        // 6. 更新手机号（调用 bindPhoneToUniqueUser，已包含缓存删除逻辑）
        bindPhoneToUniqueUser(currentUser.getUserId(), newPhone.trim());
        
        // 7. 删除验证码
        stringRedisTemplate.delete(codeKey);
        
        log.info("用户手机号修改成功: userId={}, oldPhone={}, newPhone={}", 
            currentUser.getUserId(), currentUser.getPrimaryPhone(), newPhone);
        return Result.ok("手机号修改成功");
    }

    // ========== 用户档案相关 ==========
    @Override
    @Transactional(readOnly = true)
    public Result getCurrentUser() {
            String userId = SecurityUtils.getCurrentUserId();
            if (!StringUtils.hasText(userId)) {
                throw new BusinessException("用户未登录");
            }
            // 从数据库加载完整 User（含 UserProfile），UserContextFilter 构建的最简 User 不包含 profile
            User currentUser = userRepository.findById(userId).orElse(null);
            if (currentUser == null) {
                throw new BusinessException("用户不存在");
            }
            
            // 转换为UserDTO（包含profile信息）
            UserDTO userDTO = convertToUserDTO(currentUser);
            return Result.ok(userDTO);
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
        // 绑定手机号到用户（用户自己修改手机号）
        // ⚠️ 注意：此方法目前未完全实现，如果未来需要实现，需要：
        // 1. 验证手机号格式
        // 2. 检查手机号是否已被其他用户使用
        // 3. 验证新手机号的验证码（安全考虑）
        // 4. 更新用户的primaryPhone
        // 5. 删除用户信息缓存（Cache Aside模式）
        
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("手机号不能为空");
        }
        
        // 验证手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new BusinessException("手机号格式错误");
        }
        
        // 检查手机号是否已被其他用户使用
        if (userRepository.existsByPrimaryPhone(phone)) {
            throw new BusinessException("该手机号已被其他用户使用");
        }
        
        // 查找用户
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 更新手机号
        user.setPrimaryPhone(phone.trim());
        userRepository.save(user);
        
        // ✅ Cache Aside模式：先更新数据库，再删除缓存
        if (cacheUtil != null) {
            String cacheKey = RedisConstants.CACHE_USER_INFO_KEY + userId;
            cacheUtil.delete(cacheKey);
            log.info("已删除用户信息缓存（Cache Aside模式）: userId={}, reason=primaryPhone变更, cacheKey={}", userId, cacheKey);
        }

        // ✅ Session 直写策略：phone 字段直接更新到当前 session，保持 Gateway 注入头的实时性
        syncSessionField(userId, "phone", phone.trim());
        
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
        }
        
        return userDTO;
    }
    
    // ========== 密码管理相关 ==========
    
    /**
     * 设置用户密码
     */
    public Result setPassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new BusinessException("用户不存在");
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
            throw new BusinessException("原密码不能为空");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 验证原密码
        if (user.getPassword() == null) {
            throw new BusinessException("该账户未设置密码");
        }
        if (!passwordService.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        
        // 检查新密码是否与原密码相同
        if (passwordService.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        
        // 更新密码
        String encodedPassword = passwordService.encodePassword(newPassword.trim());
        user.setPassword(encodedPassword);
        
        userRepository.save(user);
        
        // ✅ 删除用户信息缓存（密码变更后需要清除缓存）
        if (cacheUtil != null) {
            String cacheKey = RedisConstants.CACHE_USER_INFO_KEY + userId;
            cacheUtil.delete(cacheKey);
            log.info("已删除用户信息缓存: userId={}, cacheKey={}", userId, cacheKey);
        }
        
        log.info("用户密码修改成功: userId={}", userId);
        return Result.ok("密码修改成功");
    }
    
    /**
     * 重置用户密码（管理员功能）
     */
    public Result resetPassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 加密密码
        String encodedPassword = passwordService.encodePassword(newPassword.trim());
        user.setPassword(encodedPassword);
        
        userRepository.save(user);
        
        // ✅ 删除用户信息缓存（密码变更后需要清除缓存）
        if (cacheUtil != null) {
            String cacheKey = RedisConstants.CACHE_USER_INFO_KEY + userId;
            cacheUtil.delete(cacheKey);
            log.info("已删除用户信息缓存: userId={}, cacheKey={}", userId, cacheKey);
        }
        
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
     * 双Token机制：
     * - AccessToken: 短期有效（24小时），用于API访问
     * - RefreshToken: 长期有效（7天），用于刷新AccessToken
     * 
     * 策略：新登录会覆盖旧Token（单设备登录），确保Token唯一性
     * 
     * @param user 用户对象
     * @return 包含accessToken和refreshToken的TokenResultVO
     */
    /**
     * 生成并存储 Session Token（Session ID 模式）。
     *
     * 流程：
     *  1. 生成两个随机 UUID 作为 accessSessionId / refreshSessionId。
     *  2. 在 Redis 中以 Hash 结构存储 Session 数据：
     *       session:{accessSessionId}  -> {userId, username, phone, accountStatus}  TTL=24h
     *       session:refresh:{refreshSessionId} -> {userId}  TTL=7d
     *  3. JWT 中仅携带 sessionId（薄 Token），不再嵌入 userId / phone。
     *  4. 旧 Session 若存在，在写入新 Session 后顺手删除（单设备策略）。
     *
     * 下游服务不再需要 Feign 调用 auth 获取用户信息：
     *  Gateway 读取 Redis Session → 将 userId/accountStatus/username 注入请求头。
     */
    private TokenResultVO generateAndStoreTokens(User user) {
        String userId = user.getUserId();
        log.info("开始生成并存储 Session Token: userId={}", userId);

        try {
            // 1. 生成 sessionId（UUID）
            String accessSessionId = java.util.UUID.randomUUID().toString();
            String refreshSessionId = java.util.UUID.randomUUID().toString();

            // 2. 将 Session 数据写入 Redis Hash
            String sessionKey = RedisConstants.SESSION_KEY + accessSessionId;
            java.util.Map<String, String> sessionData = new java.util.LinkedHashMap<>();
            sessionData.put("userId", userId);
            sessionData.put("username", user.getUsername() != null ? user.getUsername() : "");
            sessionData.put("phone", user.getPrimaryPhone() != null ? user.getPrimaryPhone() : "");
            sessionData.put("accountStatus", user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");
            stringRedisTemplate.opsForHash().putAll(sessionKey, sessionData);
            stringRedisTemplate.expire(sessionKey, java.time.Duration.ofSeconds(RedisConstants.SESSION_TTL));

            String refreshSessionKey = RedisConstants.SESSION_REFRESH_KEY + refreshSessionId;
            stringRedisTemplate.opsForHash().put(refreshSessionKey, "userId", userId);
            stringRedisTemplate.expire(refreshSessionKey, java.time.Duration.ofSeconds(RedisConstants.SESSION_REFRESH_TTL));

            // 3. 同时在 login:token:{userId} 写入 accessSessionId（方便单设备踢出旧 Session）
            String userSessionIndex = RedisConstants.LOGIN_TOKEN_KEY + userId; // 复用旧 key 存 sessionId
            String oldSessionId = stringRedisTemplate.opsForValue().get(userSessionIndex);
            stringRedisTemplate.opsForValue().set(userSessionIndex, accessSessionId,
                    java.time.Duration.ofSeconds(RedisConstants.SESSION_TTL));

            // 4. 删除旧 Session（单设备：踢掉旧登录）
            if (oldSessionId != null && !oldSessionId.equals(accessSessionId)) {
                stringRedisTemplate.delete(RedisConstants.SESSION_KEY + oldSessionId);
                log.info("已删除旧 Session: userId={}, oldSessionId={}", userId, oldSessionId);
            }

            // 5. 生成薄 JWT（仅含 sessionId）
            String accessToken = jwtUtils.generateSessionToken(accessSessionId);
            String refreshToken = jwtUtils.generateSessionRefreshToken(refreshSessionId);

            log.info("Session Token 生成并存储完成: userId={}, accessSessionId={}, refreshSessionId={}",
                    userId, accessSessionId, refreshSessionId);

            TokenResultVO tokenResult = new TokenResultVO();
            tokenResult.setAccessToken(accessToken);
            tokenResult.setRefreshToken(refreshToken);
            tokenResult.setExpiresIn(RedisConstants.SESSION_TTL);
            return tokenResult;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Session Token 生成或存储异常: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("Token生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * Session 直写策略：将单个字段同步到当前用户的 access session。
     *
     * <p>适用于非安全敏感字段（username、phone）的用户数据变更场景。
     * 用户保持登录状态，Gateway 下次请求即可注入最新值到请求头。
     *
     * <p>安全敏感的 accountStatus 变更（封禁/停用）不走此方法，
     * 而是调用 {@code forceLogoutUser}（通过 Admin 服务），强制删除整个 session。
     */
    private void syncSessionField(String userId, String field, String value) {
        if (!org.springframework.util.StringUtils.hasText(userId)) {
            return;
        }
        try {
            String sessionIndexKey = RedisConstants.LOGIN_TOKEN_KEY + userId;
            String accessSessionId = stringRedisTemplate.opsForValue().get(sessionIndexKey);
            if (org.springframework.util.StringUtils.hasText(accessSessionId)) {
                String sessionKey = RedisConstants.SESSION_KEY + accessSessionId;
                stringRedisTemplate.opsForHash().put(sessionKey, field, value != null ? value : "");
                log.info("Session 字段已直写更新: userId={}, field={}", userId, field);
            }
        } catch (Exception e) {
            log.warn("同步 session 字段失败（非致命）: userId={}, field={}, error={}", userId, field, e.getMessage());
        }
    }

    // ========== 管理端内部方法 ==========

    @Override
    public UserInternalDTO getUserByIdInternal(String userId) {
        String cacheKey = RedisConstants.CACHE_USER_INFO_KEY + userId;
        UserInternalDTO dto = cacheUtil.getWithFallback(
            cacheKey,
            RedisConstants.CACHE_USER_INFO_TTL * 60,
            UserInternalDTO.class,
            () -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("用户不存在"));
                return userInternalDTOConverter.toInternalDTO(user);
            }
        );
        // 修复缓存中可能存在的 accountStatus 为 null 的旧数据
        if (dto != null && (dto.getAccountStatus() == null || dto.getAccountStatus().trim().isEmpty())) {
            dto.setAccountStatus("ACTIVE");
            cacheUtil.set(cacheKey, dto, RedisConstants.CACHE_USER_INFO_TTL * 60);
        }
        return dto;
    }

    @Override
    public List<UserInternalDTO> getUsersByIdsInternal(List<String> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        return userInternalDTOConverter.toUserInternalDTOList(users);
    }

    @Override
    public Map<String, Object> listUsersInternal(Integer page, Integer size,
                                                  String keyword, String accountStatus,
                                                  String sortProp, String sortOrder) {
        org.springframework.data.domain.Pageable pageable = PageRequest.of(
            Math.max(0, page - 1), size,
            Sort.by(
                "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC,
                StringUtils.hasText(sortProp) ? sortProp : "registerTime"
            )
        );

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String kw = keyword.trim();
                predicates.add(cb.or(
                    cb.like(root.get("username"), "%" + kw + "%"),
                    cb.like(root.get("primaryPhone"), "%" + kw + "%"),
                    cb.like(root.get("userId"), "%" + kw + "%")
                ));
            }
            if (StringUtils.hasText(accountStatus)) {
                predicates.add(cb.equal(root.get("accountStatus"), accountStatus.trim()));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<UserInternalDTO> userDTOs = userInternalDTOConverter.toUserInternalDTOList(userPage.getContent());

        Map<String, Object> result = new HashMap<>();
        result.put("content", userDTOs);
        result.put("totalElements", userPage.getTotalElements());
        result.put("totalPages", userPage.getTotalPages());
        result.put("number", userPage.getNumber());
        result.put("size", userPage.getSize());
        return result;
    }

    @Override
    public void updateUserFullInternal(String userId, Map<String, Object> payload) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));

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
            Set<String> allowed = new HashSet<>(Arrays.asList("ACTIVE", "SUSPENDED", "BANNED"));
            if (allowed.contains(newStatus)) {
                user.setAccountStatus(newStatus);
            }
        }

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            profile = new UserProfile();
            profile.setProfileId("PROFILE_" + System.currentTimeMillis());
            profile.setUserId(userId);
        }
        Object nickname = payload.get("nickname");
        if (nickname instanceof String && StringUtils.hasText((String) nickname)) {
            profile.setNickname(((String) nickname).trim());
        }
        Object avatar = payload.get("avatar");
        if (avatar instanceof String && StringUtils.hasText((String) avatar)) {
            profile.setAvatar(((String) avatar).trim());
        }

        userRepository.save(user);
        userProfileRepository.save(profile);

        // Cache Aside：先更新数据库，再删缓存
        cacheUtil.delete(RedisConstants.CACHE_USER_INFO_KEY + userId);
        cacheUtil.delete(RedisConstants.CACHE_USER_PROFILE_KEY + userId);
        log.info("已删除用户信息和档案缓存（Cache Aside模式）: userId={}", userId);
    }

    @Override
    public void deleteUserInternal(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setAccountStatus("DELETED");
        userRepository.save(user);

        cacheUtil.delete(RedisConstants.CACHE_USER_INFO_KEY + userId);
        cacheUtil.delete(RedisConstants.CACHE_USER_PROFILE_KEY + userId);
        log.info("已删除用户信息和档案缓存: userId={}", userId);
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

