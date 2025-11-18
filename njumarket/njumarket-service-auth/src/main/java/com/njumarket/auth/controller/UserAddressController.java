package com.njumarket.auth.controller;

import com.njumarket.auth.dto.UserAddressDTO;
import com.njumarket.auth.service.UserAddressService;
import com.njumarket.njumarket.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户地址控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/addresses")
@RequiredArgsConstructor
public class UserAddressController {
    
    private final UserAddressService userAddressService;
    
    /**
     * 创建地址
     */
    @PostMapping
    public Result createAddress(@RequestBody UserAddressDTO addressDTO) {
        return userAddressService.createAddress(addressDTO);
    }
    
    /**
     * 更新地址
     */
    @PutMapping("/{addressId}")
    public Result updateAddress(@PathVariable String addressId, 
                                @RequestBody UserAddressDTO addressDTO) {
        return userAddressService.updateAddress(addressId, addressDTO);
    }
    
    /**
     * 删除地址
     */
    @DeleteMapping("/{addressId}")
    public Result deleteAddress(@PathVariable String addressId) {
        return userAddressService.deleteAddress(addressId);
    }
    
    /**
     * 获取地址详情
     */
    @GetMapping("/{addressId}")
    public Result getAddressById(@PathVariable String addressId) {
        return userAddressService.getAddressById(addressId);
    }
    
    /**
     * 获取用户的所有地址
     */
    @GetMapping
    public Result getUserAddresses(@RequestParam(required = false) String userId) {
        return userAddressService.getUserAddresses(userId);
    }
    
    /**
     * 获取用户的默认地址
     */
    @GetMapping("/default")
    public Result getDefaultAddress(@RequestParam(required = false) String userId) {
        return userAddressService.getDefaultAddress(userId);
    }
    
    /**
     * 设置默认地址
     */
    @PutMapping("/{addressId}/default")
    public Result setDefaultAddress(@PathVariable String addressId) {
        return userAddressService.setDefaultAddress(addressId);
    }
    
    /**
     * 启用/禁用地址
     */
    @PutMapping("/{addressId}/active")
    public Result setAddressActive(@PathVariable String addressId, 
                                   @RequestParam Boolean isActive) {
        return userAddressService.setAddressActive(addressId, isActive);
    }
}

