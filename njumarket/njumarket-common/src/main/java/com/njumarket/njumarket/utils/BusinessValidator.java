package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.Admin;
import com.njumarket.njumarket.entity.Order;
import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.exception.BusinessException;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 业务校验工具类
 * 统一封装常见业务校验逻辑，减少重复代码
 */
public class BusinessValidator {
    
    /**
     * 检查用户是否登录
     * @return 当前登录用户
     * @throws BusinessException 如果用户未登录
     */
    public static User requireLogin() {
        User user = UserHolder.getUser();
        if (user == null) {
            throw new BusinessException("用户未登录");
        }
        return user;
    }
    
    /**
     * 检查字符串非空
     * @param value 要检查的值
     * @param message 错误消息
     * @throws BusinessException 如果值为空
     */
    public static void requireNotBlank(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 检查用户是否存在
     * @param userId 用户ID
     * @param repository 用户Repository
     * @return 用户对象
     * @throws BusinessException 如果用户不存在
     */
    public static User requireUser(String userId, CrudRepository<User, String> repository) {
        return repository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));
    }
    
    /**
     * 检查订单是否存在
     * @param orderId 订单ID
     * @param repository 订单Repository
     * @return 订单对象
     * @throws BusinessException 如果订单不存在
     */
    public static Order requireOrder(String orderId, CrudRepository<Order, String> repository) {
        return repository.findById(orderId)
            .orElseThrow(() -> new BusinessException("订单不存在"));
    }
    
    /**
     * 检查商品是否存在
     * @param commodityId 商品ID
     * @param repository 商品Repository
     * @return 商品对象
     * @throws BusinessException 如果商品不存在
     */
    public static Commodity requireCommodity(String commodityId, CrudRepository<Commodity, String> repository) {
        return repository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
    }
    
    /**
     * 检查是否为订单买家
     * @param order 订单对象
     * @param userId 用户ID
     * @throws BusinessException 如果不是买家
     */
    public static void requireBuyer(Order order, String userId) {
        if (order == null || !userId.equals(order.getBuyerId())) {
            throw new BusinessException("无权限操作此订单");
        }
    }
    
    /**
     * 检查是否为订单卖家
     * @param order 订单对象
     * @param userId 用户ID
     * @throws BusinessException 如果不是卖家
     */
    public static void requireSeller(Order order, String userId) {
        if (order == null || !userId.equals(order.getSellerId())) {
            throw new BusinessException("无权限操作此订单");
        }
    }
    
    /**
     * 检查是否为订单买家或卖家
     * @param order 订单对象
     * @param userId 用户ID
     * @throws BusinessException 如果不是买家或卖家
     */
    public static void requireBuyerOrSeller(Order order, String userId) {
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        boolean isBuyer = userId.equals(order.getBuyerId());
        boolean isSeller = userId.equals(order.getSellerId());
        if (!isBuyer && !isSeller) {
            throw new BusinessException("无权限操作此订单");
        }
    }
    
    /**
     * 检查是否为商品所有者
     * @param commodity 商品对象
     * @param userId 用户ID
     * @throws BusinessException 如果不是所有者
     */
    public static void requireCommodityOwner(Commodity commodity, String userId) {
        if (commodity == null || !userId.equals(commodity.getSellerId())) {
            throw new BusinessException("无权限操作此商品");
        }
    }
    
    /**
     * 检查订单状态
     * @param order 订单对象
     * @param allowedStatuses 允许的状态列表
     * @throws BusinessException 如果状态不允许
     */
    public static void requireOrderStatus(Order order, String... allowedStatuses) {
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        String currentStatus = order.getOrderStatus();
        for (String status : allowedStatuses) {
            if (status.equals(currentStatus)) {
                return;
            }
        }
        throw new BusinessException("订单状态不允许此操作");
    }
    
    /**
     * 检查商品状态
     * @param commodity 商品对象
     * @param allowedStatuses 允许的状态列表
     * @throws BusinessException 如果状态不允许
     */
    public static void requireCommodityStatus(Commodity commodity, String... allowedStatuses) {
        if (commodity == null) {
            throw new BusinessException("商品不存在");
        }
        String currentStatus = commodity.getCommodityStatus();
        for (String status : allowedStatuses) {
            if (status.equals(currentStatus)) {
                return;
            }
        }
        throw new BusinessException("商品状态不允许此操作");
    }
    
    /**
     * 检查用户账户状态
     * @param user 用户对象
     * @throws BusinessException 如果账户未激活
     */
    public static void requireActiveUser(User user) {
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new BusinessException("用户账户未激活");
        }
    }
    
    /**
     * 检查管理员是否存在
     * @param adminId 管理员ID
     * @param repository 管理员Repository
     * @return 管理员对象
     * @throws BusinessException 如果管理员不存在
     */
    public static Admin requireAdmin(String adminId, CrudRepository<Admin, String> repository) {
        return repository.findById(adminId)
            .orElseThrow(() -> new BusinessException("管理员不存在"));
    }
    
    /**
     * 检查管理员账户状态
     * @param admin 管理员对象
     * @throws BusinessException 如果账户未激活
     */
    public static void requireActiveAdmin(Admin admin) {
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (!"ACTIVE".equals(admin.getAccountStatus())) {
            throw new BusinessException("管理员账户未激活");
        }
    }
    
    /**
     * 检查商品是否有订单
     * @param commodityId 商品ID
     * @param orderRepository 订单Repository（需要实现findByCommodityId方法）
     * @throws BusinessException 如果商品有订单
     * 
     * 注意：此方法需要OrderRepository实现findByCommodityId方法
     * 如果Repository没有此方法，请在调用处直接使用Repository的findByCommodityId方法
     */
    public static void requireNoOrders(String commodityId, CrudRepository<Order, String> orderRepository) {
        // 注意：CrudRepository没有findByCommodityId方法，此方法需要在实际使用时
        // 通过具体的Repository接口（如OrderRepository）调用findByCommodityId方法
        // 这里提供一个通用实现，但效率较低
        Iterable<Order> allOrders = orderRepository.findAll();
        for (Order order : allOrders) {
            if (commodityId.equals(order.getCommodityId())) {
                throw new BusinessException("该商品已有订单，无法删除");
            }
        }
    }
}

