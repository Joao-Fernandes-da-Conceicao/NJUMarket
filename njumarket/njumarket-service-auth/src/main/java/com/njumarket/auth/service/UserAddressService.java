package com.njumarket.auth.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.auth.dto.UserAddressDTO;

/**
 * 用户地址服务接口
 */
public interface UserAddressService {
    
    /**
     * 创建地址
     * @param addressDTO 地址信息
     * @return 创建结果
     */
    Result createAddress(UserAddressDTO addressDTO);
    
    /**
     * 更新地址
     * @param addressId 地址ID
     * @param addressDTO 地址信息
     * @return 更新结果
     */
    Result updateAddress(String addressId, UserAddressDTO addressDTO);
    
    /**
     * 删除地址
     * @param addressId 地址ID
     * @return 删除结果
     */
    Result deleteAddress(String addressId);
    
    /**
     * 获取地址详情
     * @param addressId 地址ID
     * @return 地址信息
     */
    Result getAddressById(String addressId);
    
    /**
     * 获取用户的所有地址
     * @param userId 用户ID（可选，如果不传则获取当前用户）
     * @return 地址列表
     */
    Result getUserAddresses(String userId);
    
    /**
     * 获取用户的默认地址
     * @param userId 用户ID（可选，如果不传则获取当前用户）
     * @return 默认地址
     */
    Result getDefaultAddress(String userId);
    
    /**
     * 设置默认地址
     * @param addressId 地址ID
     * @return 设置结果
     */
    Result setDefaultAddress(String addressId);
    
    /**
     * 启用/禁用地址
     * @param addressId 地址ID
     * @param isActive 是否启用
     * @return 操作结果
     */
    Result setAddressActive(String addressId, Boolean isActive);
    
    // ========== 内部方法（供其他服务调用）==========
    
    /**
     * 根据地址ID获取地址信息（内部调用）
     * @param addressId 地址ID
     * @return 地址DTO
     */
    UserAddressDTO getAddressByIdInternal(String addressId);
    
    /**
     * 获取用户的默认地址（内部调用）
     * @param userId 用户ID
     * @return 地址DTO
     */
    UserAddressDTO getDefaultAddressInternal(String userId);
}

