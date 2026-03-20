package com.njumarket.auth.service.impl;

import com.njumarket.auth.dto.UserAddressDTO;
import com.njumarket.auth.dto.internal.AddressInternalDTOConverter;
import com.njumarket.auth.entity.UserAddress;
import com.njumarket.auth.repository.UserAddressRepository;
import com.njumarket.auth.service.UserAddressService;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.AddressInternalDTO;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户地址服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {
    
    private final UserAddressRepository userAddressRepository;
    private final AddressInternalDTOConverter addressInternalDTOConverter;
    
    @Override
    @Transactional
    public Result createAddress(UserAddressDTO addressDTO) {
        // 1. 获取当前用户ID
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        
        // 2. 参数验证
        validateAddressDTO(addressDTO);
        
        // 3. 构建地址实体
        UserAddress address = new UserAddress();
        address.setAddressId("ADDR" + userId + "_" + System.currentTimeMillis());
        address.setUserId(userId);
        address.setRecipientName(addressDTO.getRecipientName());
        address.setRecipientPhone(addressDTO.getRecipientPhone());
        address.setProvince(addressDTO.getProvince());
        address.setCity(addressDTO.getCity());
        address.setDistrict(addressDTO.getDistrict());
        address.setStreetAddress(addressDTO.getStreetAddress());
        address.setDetailAddress(addressDTO.getDetailAddress());
        
        // 构建完整地址
        String fullAddress = buildFullAddress(addressDTO);
        address.setFullAddress(fullAddress);
        
        // 设置地理位置
        if (addressDTO.getLongitude() != null && addressDTO.getLatitude() != null) {
            address.setLocation(addressDTO.getLongitude(), addressDTO.getLatitude());
        }
        
        // 设置地址标签
        address.setAddressLabel(addressDTO.getAddressLabel() != null ? 
            addressDTO.getAddressLabel() : "HOME");
        
        // 4. 处理默认地址
        Boolean isDefault = addressDTO.getIsDefault() != null ? addressDTO.getIsDefault() : false;
        if (isDefault) {
            // 取消其他默认地址
            userAddressRepository.clearDefaultAddress(userId);
        }
        address.setIsDefault(isDefault);
        address.setIsActive(addressDTO.getIsActive() != null ? addressDTO.getIsActive() : true);
        
        // 5. 保存地址
        UserAddress savedAddress = userAddressRepository.save(address);
        
        log.info("用户创建地址成功: userId={}, addressId={}", userId, savedAddress.getAddressId());
        return Result.ok(convertToDTO(savedAddress));
    }
    
    @Override
    @Transactional
    public Result updateAddress(String addressId, UserAddressDTO addressDTO) {
        // 1. 获取当前用户ID
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        
        // 2. 查询地址并验证权限
        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, userId)
            .orElseThrow(() -> new BusinessException("地址不存在或无权限"));
        
        // 3. 参数验证
        validateAddressDTO(addressDTO);
        
        // 4. 更新地址信息
        address.setRecipientName(addressDTO.getRecipientName());
        address.setRecipientPhone(addressDTO.getRecipientPhone());
        address.setProvince(addressDTO.getProvince());
        address.setCity(addressDTO.getCity());
        address.setDistrict(addressDTO.getDistrict());
        address.setStreetAddress(addressDTO.getStreetAddress());
        address.setDetailAddress(addressDTO.getDetailAddress());
        
        // 构建完整地址
        String fullAddress = buildFullAddress(addressDTO);
        address.setFullAddress(fullAddress);
        
        // 更新地理位置
        if (addressDTO.getLongitude() != null && addressDTO.getLatitude() != null) {
            address.setLocation(addressDTO.getLongitude(), addressDTO.getLatitude());
        }
        
        // 更新地址标签
        if (addressDTO.getAddressLabel() != null) {
            address.setAddressLabel(addressDTO.getAddressLabel());
        }
        
        // 5. 处理默认地址
        if (addressDTO.getIsDefault() != null && addressDTO.getIsDefault()) {
            // 取消其他默认地址
            userAddressRepository.clearDefaultAddress(userId);
            address.setIsDefault(true);
        }
        
        // 更新启用状态
        if (addressDTO.getIsActive() != null) {
            address.setIsActive(addressDTO.getIsActive());
        }
        
        // 6. 保存更新
        UserAddress updatedAddress = userAddressRepository.save(address);
        
        log.info("用户更新地址成功: userId={}, addressId={}", userId, addressId);
        return Result.ok(convertToDTO(updatedAddress));
    }
    
    @Override
    @Transactional
    public Result deleteAddress(String addressId) {
        // 1. 获取当前用户ID
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        
        // 2. 查询地址并验证权限
        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, userId)
            .orElseThrow(() -> new BusinessException("地址不存在或无权限"));
        
        // 3. 删除地址（软删除：设置为不启用）
        address.setIsActive(false);
        userAddressRepository.save(address);
        
        log.info("用户删除地址成功: userId={}, addressId={}", userId, addressId);
        return Result.ok("地址删除成功", null);
    }
    
    @Override
    public Result getAddressById(String addressId) {
        // 1. 获取当前用户ID
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        
        // 2. 查询地址并验证权限
        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, userId)
            .orElseThrow(() -> new BusinessException("地址不存在或无权限"));
        
        return Result.ok(convertToDTO(address));
    }
    
    @Override
    public Result getUserAddresses(String userId) {
        // 1. 确定用户ID
        String targetUserId = userId != null ? userId : SecurityUtils.getCurrentUserId();
        if (targetUserId == null) {
            throw new BusinessException("用户未登录");
        }
        
        // 2. 查询用户的所有启用地址
        List<UserAddress> addresses = userAddressRepository.findByUserIdAndIsActive(targetUserId, true);
        
        // 3. 转换为DTO列表
        List<UserAddressDTO> addressDTOs = addresses.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return Result.ok(addressDTOs);
    }
    
    @Override
    public Result getDefaultAddress(String userId) {
        // 1. 确定用户ID
        String targetUserId = userId != null ? userId : SecurityUtils.getCurrentUserId();
        if (targetUserId == null) {
            throw new BusinessException("用户未登录");
        }
        
        // 2. 查询默认地址
        UserAddress address = userAddressRepository
            .findByUserIdAndIsDefaultTrueAndIsActiveTrue(targetUserId)
            .orElse(null);
        
        if (address == null) {
            return Result.ok(null);
        }
        
        return Result.ok(convertToDTO(address));
    }
    
    @Override
    @Transactional
    public Result setDefaultAddress(String addressId) {
        // 1. 获取当前用户ID
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        
        // 2. 查询地址并验证权限
        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, userId)
            .orElseThrow(() -> new BusinessException("地址不存在或无权限"));
        
        // 3. 取消其他默认地址
        userAddressRepository.clearDefaultAddress(userId);
        
        // 4. 设置当前地址为默认
        address.setIsDefault(true);
        userAddressRepository.save(address);
        
        log.info("用户设置默认地址成功: userId={}, addressId={}", userId, addressId);
        return Result.ok("设置默认地址成功", null);
    }
    
    @Override
    @Transactional
    public Result setAddressActive(String addressId, Boolean isActive) {
        // 1. 获取当前用户ID
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        
        // 2. 查询地址并验证权限
        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, userId)
            .orElseThrow(() -> new BusinessException("地址不存在或无权限"));
        
        // 3. 更新启用状态
        address.setIsActive(isActive);
        userAddressRepository.save(address);
        
        log.info("用户更新地址启用状态: userId={}, addressId={}, isActive={}", userId, addressId, isActive);
        return Result.ok("更新成功", null);
    }
    
    // ========== 内部方法 ==========
    
    @Override
    public AddressInternalDTO getAddressByIdInternal(String addressId) {
        UserAddress address = userAddressRepository.findById(addressId)
            .orElseThrow(() -> new BusinessException("地址不存在"));
        return addressInternalDTOConverter.toInternalDTO(address);
    }

    @Override
    public AddressInternalDTO getDefaultAddressInternal(String userId) {
        return userAddressRepository
            .findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
            .map(addressInternalDTOConverter::toInternalDTO)
            .orElse(null);
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 验证地址DTO
     */
    private void validateAddressDTO(UserAddressDTO addressDTO) {
        if (!StringUtils.hasText(addressDTO.getRecipientName())) {
            throw new BusinessException("收货人姓名不能为空");
        }
        if (!StringUtils.hasText(addressDTO.getRecipientPhone())) {
            throw new BusinessException("收货人电话不能为空");
        }
        if (!StringUtils.hasText(addressDTO.getProvince())) {
            throw new BusinessException("省份不能为空");
        }
        if (!StringUtils.hasText(addressDTO.getCity())) {
            throw new BusinessException("城市不能为空");
        }
        if (!StringUtils.hasText(addressDTO.getDistrict())) {
            throw new BusinessException("区/县不能为空");
        }
        if (!StringUtils.hasText(addressDTO.getStreetAddress())) {
            throw new BusinessException("街道地址不能为空");
        }
    }
    
    /**
     * 构建完整地址
     */
    private String buildFullAddress(UserAddressDTO addressDTO) {
        StringBuilder sb = new StringBuilder();
        sb.append(addressDTO.getProvince())
          .append(addressDTO.getCity())
          .append(addressDTO.getDistrict())
          .append(addressDTO.getStreetAddress());
        
        if (StringUtils.hasText(addressDTO.getDetailAddress())) {
            sb.append(addressDTO.getDetailAddress());
        }
        
        return sb.toString();
    }
    
    /**
     * 实体转DTO
     */
    private UserAddressDTO convertToDTO(UserAddress address) {
        UserAddressDTO dto = new UserAddressDTO();
        dto.setAddressId(address.getAddressId());
        dto.setUserId(address.getUserId());
        dto.setRecipientName(address.getRecipientName());
        dto.setRecipientPhone(address.getRecipientPhone());
        dto.setProvince(address.getProvince());
        dto.setCity(address.getCity());
        dto.setDistrict(address.getDistrict());
        dto.setStreetAddress(address.getStreetAddress());
        dto.setDetailAddress(address.getDetailAddress());
        dto.setFullAddress(address.getFullAddress());
        dto.setLongitude(address.getLongitude());
        dto.setLatitude(address.getLatitude());
        dto.setAddressLabel(address.getAddressLabel());
        dto.setIsDefault(address.getIsDefault());
        dto.setIsActive(address.getIsActive());
        dto.setCreateTime(address.getCreateTime());
        dto.setUpdateTime(address.getUpdateTime());
        return dto;
    }
}

