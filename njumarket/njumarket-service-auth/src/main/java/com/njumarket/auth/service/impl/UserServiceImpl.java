package com.njumarket.auth.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.auth.dto.UserDTO;
import com.njumarket.auth.dto.LoginFormDTO;
import com.njumarket.auth.dto.RegisterDTO;
import com.njumarket.auth.vo.LoginResultVO;
import com.njumarket.auth.vo.TokenResultVO;
import com.njumarket.auth.entity.User;
import com.njumarket.auth.entity.UserProfile;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
    private final CacheUtil cacheUtil;
    private final com.njumarket.auth.vector.UserProfileVectorService userProfileVectorService;

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
        
        // ✅ v3.1.0: 登录成功后异步生成用户画像向量（降低API调用成本）
        try {
            if (userProfileVectorService != null) {
                userProfileVectorService.generateAndStoreUserProfileVector(user.getUserId());
                log.debug("登录后触发用户画像生成: userId={}", user.getUserId());
            }
        } catch (Exception e) {
            // 画像生成失败不影响登录流程，只记录日志
            log.warn("登录后触发用户画像生成失败: userId={}, error={}", user.getUserId(), e.getMessage());
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
        
        // ✅ v3.1.0: 登录成功后异步生成用户画像向量（降低API调用成本）
        try {
            if (userProfileVectorService != null) {
                userProfileVectorService.generateAndStoreUserProfileVector(user.getUserId());
                log.debug("登录后触发用户画像生成: userId={}", user.getUserId());
            }
        } catch (Exception e) {
            // 画像生成失败不影响登录流程，只记录日志
            log.warn("登录后触发用户画像生成失败: userId={}, error={}", user.getUserId(), e.getMessage());
        }
        
        return Result.ok(result);
    }

    @Override
    public Result loginThirdParty(String type, String code, HttpSession session) {
        // TODO:实现第三方登录逻辑
        return Result.ok("登录成功");
    }

    @Override
    public Result logout(HttpSession session) {
        // 1. 获取当前用户（使用 SecurityUtils）
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        String userId = currentUser.getUserId();
        
        log.info("用户登出开始: userId={}", userId);
        
        // 2. 删除Redis中的AccessToken
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + userId;
        String oldAccessToken = stringRedisTemplate.opsForValue().get(tokenKey);
        Boolean accessTokenDeleted = stringRedisTemplate.delete(tokenKey);
        log.info("删除AccessToken: userId={}, tokenKey={}, oldToken存在={}, deleted={}", 
            userId, tokenKey, oldAccessToken != null, accessTokenDeleted);
        
        // 验证AccessToken是否已删除
        String verifyAccessToken = stringRedisTemplate.opsForValue().get(tokenKey);
        if (verifyAccessToken != null) {
            log.error("AccessToken删除失败，仍然存在: userId={}, tokenKey={}", userId, tokenKey);
            // 强制删除
            stringRedisTemplate.delete(tokenKey);
        }
        
        // 3. 删除Redis中的RefreshToken
        String refreshKey = RedisConstants.REFRESH_TOKEN_KEY + userId;
        String oldRefreshToken = stringRedisTemplate.opsForValue().get(refreshKey);
        Boolean refreshTokenDeleted = stringRedisTemplate.delete(refreshKey);
        log.info("删除RefreshToken: userId={}, refreshKey={}, oldToken存在={}, deleted={}", 
            userId, refreshKey, oldRefreshToken != null, refreshTokenDeleted);
        
        // 验证RefreshToken是否已删除
        String verifyRefreshToken = stringRedisTemplate.opsForValue().get(refreshKey);
        if (verifyRefreshToken != null) {
            log.error("RefreshToken删除失败，仍然存在: userId={}, refreshKey={}", userId, refreshKey);
            // 强制删除
            stringRedisTemplate.delete(refreshKey);
        }
        
        // 4. 清除ThreadLocal中的用户信息
        SecurityUtils.clearContext();
        
        log.info("用户登出成功: userId={}, accessTokenDeleted={}, refreshTokenDeleted={}", 
            userId, accessTokenDeleted, refreshTokenDeleted);
        return Result.ok("登出成功");
    }

    @Override
    public Result refreshToken(String refreshToken) {
        // 1. 验证刷新Token（JWT本身）
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException("刷新Token无效或已过期");
        }
        
        // 2. 检查是否为刷新Token
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw new BusinessException("Token类型错误，请使用RefreshToken");
        }
        
        // 3. 从刷新Token中获取用户ID
        String userId = jwtUtils.getUserIdFromToken(refreshToken);
        if (userId == null) {
            throw new BusinessException("无法从Token中获取用户信息");
        }
        
        // 4. 验证Redis中的RefreshToken（检查是否被撤销）
        String refreshKey = RedisConstants.REFRESH_TOKEN_KEY + userId;
        String cachedRefreshToken = stringRedisTemplate.opsForValue().get(refreshKey);
        
        if (!StringUtils.hasText(cachedRefreshToken)) {
            throw new BusinessException("RefreshToken已被撤销，请重新登录");
        }
        
        // 5. 验证Redis中的RefreshToken是否与请求中的一致
        if (!refreshToken.equals(cachedRefreshToken)) {
            throw new BusinessException("RefreshToken不匹配，可能在其他设备登录");
        }
        
        // 6. 查询用户信息
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 7. 检查用户状态
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new BusinessException("账户已被禁用");
        }
        
        // 8. 生成新的AccessToken和RefreshToken
        String newAccessToken = jwtUtils.generateToken(user.getUserId(), user.getPrimaryPhone());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getUserId());
        
        log.info("刷新Token: userId={}, newAccessToken前10字符={}..., newRefreshToken前10字符={}...", 
            user.getUserId(), 
            newAccessToken.length() > 10 ? newAccessToken.substring(0, 10) : newAccessToken,
            newRefreshToken.length() > 10 ? newRefreshToken.substring(0, 10) : newRefreshToken);
        
        // 9. 更新Redis中的AccessToken（使用set覆盖写入，确保更新）
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + user.getUserId();
        
        // 检查是否存在旧AccessToken（用于日志记录）
        String oldAccessToken = stringRedisTemplate.opsForValue().get(tokenKey);
        boolean hasOldAccessToken = oldAccessToken != null;
        
        // 使用set方法直接覆盖写入（不是setIfAbsent，确保更新）
        stringRedisTemplate.opsForValue().set(tokenKey, newAccessToken, 
            RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        
        // 验证AccessToken写入是否成功
        String storedNewToken = stringRedisTemplate.opsForValue().get(tokenKey);
        if (storedNewToken == null) {
            log.error("刷新Token时AccessToken写入Redis失败（读取为null）: userId={}, tokenKey={}", user.getUserId(), tokenKey);
            throw new BusinessException("Token刷新失败：AccessToken写入失败");
        }
        if (!storedNewToken.equals(newAccessToken)) {
            log.error("刷新Token时AccessToken写入Redis失败（值不匹配）: userId={}, tokenKey={}, 期望长度={}, 实际长度={}", 
                user.getUserId(), tokenKey, newAccessToken.length(), storedNewToken.length());
            throw new BusinessException("Token刷新失败：AccessToken值不匹配");
        }
        log.info("刷新Token时AccessToken写入Redis成功: userId={}, tokenKey={}, TTL={}分钟, 覆盖旧Token={}", 
            user.getUserId(), tokenKey, RedisConstants.LOGIN_TOKEN_TTL, hasOldAccessToken);
        
        // 10. 更新Redis中的RefreshToken（使用set覆盖写入，确保更新）
        // 检查是否存在旧RefreshToken（用于日志记录）
        String oldRefreshToken = stringRedisTemplate.opsForValue().get(refreshKey);
        boolean hasOldRefreshToken = oldRefreshToken != null;
        
        // 使用set方法直接覆盖写入（不是setIfAbsent，确保更新）
        stringRedisTemplate.opsForValue().set(refreshKey, newRefreshToken, 
            RedisConstants.REFRESH_TOKEN_TTL, TimeUnit.MINUTES);
        
        // 验证RefreshToken写入是否成功
        String storedNewRefreshToken = stringRedisTemplate.opsForValue().get(refreshKey);
        if (storedNewRefreshToken == null) {
            log.error("刷新Token时RefreshToken写入Redis失败（读取为null）: userId={}, refreshKey={}", user.getUserId(), refreshKey);
            throw new BusinessException("Token刷新失败：RefreshToken写入失败");
        }
        if (!storedNewRefreshToken.equals(newRefreshToken)) {
            log.error("刷新Token时RefreshToken写入Redis失败（值不匹配）: userId={}, refreshKey={}, 期望长度={}, 实际长度={}", 
                user.getUserId(), refreshKey, newRefreshToken.length(), storedNewRefreshToken.length());
            throw new BusinessException("Token刷新失败：RefreshToken值不匹配");
        }
        log.info("刷新Token时RefreshToken写入Redis成功: userId={}, refreshKey={}, TTL={}分钟, 覆盖旧Token={}", 
            user.getUserId(), refreshKey, RedisConstants.REFRESH_TOKEN_TTL, hasOldRefreshToken);
        
        // 11. 返回新Token
        TokenResultVO result = new TokenResultVO();
        result.setAccessToken(newAccessToken);
        result.setRefreshToken(newRefreshToken);
        result.setExpiresIn(RedisConstants.LOGIN_TOKEN_TTL * 60L); // 转换为秒
        
        log.debug("Token刷新成功: userId={}", userId);
        return Result.ok(result);
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
    public Result getCurrentUser() {
            // 获取当前用户信息（使用 SecurityUtils）
            Object userObj = SecurityUtils.getCurrentUser();
            User currentUser = userObj instanceof User ? (User) userObj : null;
            if (currentUser == null) {
            throw new BusinessException("用户未登录");
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
        // 删除用户信息缓存（primaryPhone变更后需要清除缓存）
        if (cacheUtil != null) {
            String cacheKey = RedisConstants.CACHE_USER_INFO_KEY + userId;
            cacheUtil.delete(cacheKey);
            log.info("已删除用户信息缓存（Cache Aside模式）: userId={}, reason=primaryPhone变更, cacheKey={}", userId, cacheKey);
        }
        
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
    private TokenResultVO generateAndStoreTokens(User user) {
        String userId = user.getUserId();
        log.info("开始生成并存储Token: userId={}", userId);
        
        try {
            // 1. 生成AccessToken和RefreshToken（每次生成都是新的，因为issuedAt时间不同）
            String accessToken = jwtUtils.generateToken(userId, user.getPrimaryPhone());
            String refreshToken = jwtUtils.generateRefreshToken(userId);
            
            log.info("Token生成完成: userId={}, accessToken长度={}, refreshToken长度={}", 
                userId, accessToken.length(), refreshToken.length());
            
            // 2. 存储AccessToken到Redis（使用set覆盖写入，确保更新）
            String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + userId;
            
            // 检查是否存在旧Token（用于日志记录）
            String oldAccessToken = stringRedisTemplate.opsForValue().get(tokenKey);
            boolean hasOldToken = oldAccessToken != null;
            
            if (hasOldToken && oldAccessToken != null) {
                log.info("检测到旧AccessToken，将被覆盖: userId={}, tokenKey={}, 旧Token前10字符={}...", 
                    userId, tokenKey, 
                    oldAccessToken.length() > 10 ? oldAccessToken.substring(0, 10) : oldAccessToken);
            }
            
            // 使用set方法直接覆盖写入（不是setIfAbsent，确保更新）
            stringRedisTemplate.opsForValue().set(tokenKey, accessToken, 
                RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
            
            // 验证写入是否成功
            String storedAccessToken = stringRedisTemplate.opsForValue().get(tokenKey);
            if (storedAccessToken == null) {
                log.error("AccessToken写入Redis失败（读取为null）: userId={}, tokenKey={}", userId, tokenKey);
                throw new RuntimeException("AccessToken写入Redis失败：读取为null");
            }
            if (!storedAccessToken.equals(accessToken)) {
                log.error("AccessToken写入Redis失败（值不匹配）: userId={}, tokenKey={}, 期望长度={}, 实际长度={}", 
                    userId, tokenKey, accessToken.length(), storedAccessToken.length());
                throw new RuntimeException("AccessToken写入Redis失败：值不匹配");
            }
            
            log.info("AccessToken存储成功: userId={}, tokenKey={}, TTL={}分钟, 覆盖旧Token={}", 
                userId, tokenKey, RedisConstants.LOGIN_TOKEN_TTL, hasOldToken);
            
            // 3. 存储RefreshToken到Redis（使用set覆盖写入，确保更新）
            String refreshKey = RedisConstants.REFRESH_TOKEN_KEY + userId;
            
            // 检查是否存在旧RefreshToken（用于日志记录）
            String oldRefreshToken = stringRedisTemplate.opsForValue().get(refreshKey);
            boolean hasOldRefreshToken = oldRefreshToken != null;
            
            if (hasOldRefreshToken) {
                log.info("检测到旧RefreshToken，将被覆盖: userId={}, refreshKey={}", userId, refreshKey);
            }
            
            // 使用set方法直接覆盖写入（不是setIfAbsent，确保更新）
            stringRedisTemplate.opsForValue().set(refreshKey, refreshToken, 
                RedisConstants.REFRESH_TOKEN_TTL, TimeUnit.MINUTES);
            
            // 验证RefreshToken写入是否成功
            String storedRefreshToken = stringRedisTemplate.opsForValue().get(refreshKey);
            if (storedRefreshToken == null) {
                log.error("RefreshToken写入Redis失败（读取为null）: userId={}, refreshKey={}", userId, refreshKey);
                throw new RuntimeException("RefreshToken写入Redis失败：读取为null");
            }
            if (!storedRefreshToken.equals(refreshToken)) {
                log.error("RefreshToken写入Redis失败（值不匹配）: userId={}, refreshKey={}, 期望长度={}, 实际长度={}", 
                    userId, refreshKey, refreshToken.length(), storedRefreshToken.length());
                throw new RuntimeException("RefreshToken写入Redis失败：值不匹配");
            }
            
            log.info("RefreshToken存储成功: userId={}, refreshKey={}, TTL={}分钟, 覆盖旧Token={}", 
                userId, refreshKey, RedisConstants.REFRESH_TOKEN_TTL, hasOldRefreshToken);
            
            // 4. 返回Token信息
            TokenResultVO tokenResult = new TokenResultVO();
            tokenResult.setAccessToken(accessToken);
            tokenResult.setRefreshToken(refreshToken);
            tokenResult.setExpiresIn(RedisConstants.LOGIN_TOKEN_TTL * 60L); // 转换为秒
            
            log.info("双Token机制：Token生成并存储完成: userId={}, accessTokenKey={}, refreshTokenKey={}, 覆盖旧Token={}", 
                userId, tokenKey, refreshKey, (hasOldToken || hasOldRefreshToken));
            
            return tokenResult;
            
        } catch (RuntimeException e) {
            // 重新抛出RuntimeException
            throw e;
        } catch (Exception e) {
            log.error("Token生成或存储异常: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("Token生成失败: " + e.getMessage(), e);
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

