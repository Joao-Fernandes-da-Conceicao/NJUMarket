package com.njumarket.auth.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.AddressInternalDTO;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.auth.service.UserAddressService;
import com.njumarket.auth.service.UserProfileService;
import com.njumarket.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 内部API控制器
 * 用于微服务间调用，不对外暴露。所有业务逻辑均由 Service 层承载。
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final UserAddressService userAddressService;

    // ========== 用户查询 ==========

    @GetMapping("/user/{userId}")
    public Result getUserById(@PathVariable String userId) {
        UserInternalDTO dto = userService.getUserByIdInternal(userId);
        return Result.ok("查询成功", dto);
    }

    @GetMapping("/user/batch")
    public Result getUsersByIds(@RequestParam List<String> userIds) {
        List<UserInternalDTO> dtos = userService.getUsersByIdsInternal(userIds);
        return Result.ok("批量查询成功", dtos);
    }

    @GetMapping("/users")
    public Result listUsers(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer size,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String accountStatus,
                            @RequestParam(required = false) String sortProp,
                            @RequestParam(required = false) String sortOrder) {
        Map<String, Object> result = userService.listUsersInternal(page, size, keyword, accountStatus, sortProp, sortOrder);
        return Result.ok("查询成功", result);
    }

    // ========== 用户档案查询 ==========

    @GetMapping("/user/profile/batch")
    public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
        List<UserProfileInternalDTO> dtos = userProfileService.getUserProfilesByIdsInternal(userIds);
        return Result.ok("批量查询成功", dtos);
    }

    // ========== 用户管理（管理端） ==========

    @PutMapping("/user/{userId}/full")
    public Result updateUserFull(@PathVariable String userId, @RequestBody Map<String, Object> payload) {
        userService.updateUserFullInternal(userId, payload);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/user/{userId}")
    public Result deleteUser(@PathVariable String userId) {
        userService.deleteUserInternal(userId);
        return Result.ok("删除成功");
    }

    // ========== 订单提醒 ==========

    @PutMapping("/user/{userId}/order-reminder")
    public Result setOrderReminderStatus(@PathVariable String userId,
                                         @RequestParam String role,
                                         @RequestParam Boolean hasNew) {
        userProfileService.setOrderReminderStatus(userId, role, hasNew);
        return Result.ok("设置成功");
    }

    // ========== 地址查询 ==========

    @GetMapping("/address/{addressId}")
    public Result getAddressById(@PathVariable String addressId) {
        AddressInternalDTO dto = userAddressService.getAddressByIdInternal(addressId);
        return Result.ok("查询成功", dto);
    }

    @GetMapping("/address/default")
    public Result getDefaultAddress(@RequestParam String userId) {
        AddressInternalDTO dto = userAddressService.getDefaultAddressInternal(userId);
        return Result.ok("查询成功", dto);
    }
}
