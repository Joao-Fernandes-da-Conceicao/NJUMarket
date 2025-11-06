package com.njumarket.njumarket.utils;

import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.entity.Order;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.repository.CommodityRepository;
import com.njumarket.njumarket.repository.OrderRepository;
import com.njumarket.njumarket.repository.UserRepository;

import java.util.List;
import java.util.Optional;

/**
 * 业务校验工具类
 * 抽取通用的业务校验逻辑，减少重复代码
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
     * 检查订单是否存在
     * @param orderId 订单ID
     * @param repository 订单Repository
     * @return 订单对象
     * @throws BusinessException 如果订单不存在
     */
    public static Order requireOrder(String orderId, OrderRepository repository) {
        Optional<Order> orderOpt = repository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new BusinessException("订单不存在");
        }
        return orderOpt.get();
    }
    
    /**
     * 检查商品是否存在
     * @param commodityId 商品ID
     * @param repository 商品Repository
     * @return 商品对象
     * @throws BusinessException 如果商品不存在
     */
    public static Commodity requireCommodity(String commodityId, CommodityRepository repository) {
        Optional<Commodity> commodityOpt = repository.findById(commodityId);
        if (commodityOpt.isEmpty()) {
            throw new BusinessException("商品不存在");
        }
        return commodityOpt.get();
    }
    
    /**
     * 检查用户是否存在
     * @param userId 用户ID
     * @param repository 用户Repository
     * @return 用户对象
     * @throws BusinessException 如果用户不存在
     */
    public static User requireUser(String userId, UserRepository repository) {
        Optional<User> userOpt = repository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new BusinessException("用户不存在");
        }
        return userOpt.get();
    }
    
    /**
     * 检查是否为订单买家
     * @param order 订单对象
     * @param userId 用户ID
     * @throws BusinessException 如果不是买家
     */
    public static void requireBuyer(Order order, String userId) {
        if (!order.getBuyerId().equals(userId)) {
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
        if (!order.getSellerId().equals(userId)) {
            throw new BusinessException("无权限操作此订单");
        }
    }
    
    /**
     * 检查是否为订单的买家或卖家
     * @param order 订单对象
     * @param userId 用户ID
     * @throws BusinessException 如果不是买家或卖家
     */
    public static void requireBuyerOrSeller(Order order, String userId) {
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException("无权限查看此订单");
        }
    }
    
    /**
     * 检查订单状态是否允许操作
     * @param order 订单对象
     * @param allowedStatuses 允许的状态列表
     * @throws BusinessException 如果状态不允许
     */
    public static void requireOrderStatus(Order order, String... allowedStatuses) {
        String currentStatus = order.getOrderStatus();
        for (String status : allowedStatuses) {
            if (status.equals(currentStatus)) {
                return;
            }
        }
        throw new BusinessException("订单状态不允许此操作");
    }
    
    /**
     * 检查商品状态是否允许操作
     * @param commodity 商品对象
     * @param allowedStatuses 允许的状态列表
     * @throws BusinessException 如果状态不允许
     */
    public static void requireCommodityStatus(Commodity commodity, String... allowedStatuses) {
        String currentStatus = commodity.getCommodityStatus();
        for (String status : allowedStatuses) {
            if (status.equals(currentStatus)) {
                return;
            }
        }
        throw new BusinessException("商品状态不允许此操作");
    }
    
    /**
     * 检查用户账户状态是否激活
     * @param user 用户对象
     * @throws BusinessException 如果账户未激活
     */
    public static void requireActiveUser(User user) {
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new BusinessException("账户已被禁用");
        }
    }
    
    /**
     * 检查是否为商品所有者
     * @param commodity 商品对象
     * @param userId 用户ID
     * @throws BusinessException 如果不是所有者
     */
    public static void requireCommodityOwner(Commodity commodity, String userId) {
        if (!commodity.getSellerId().equals(userId)) {
            throw new BusinessException("无权限操作此商品");
        }
    }
    
    /**
     * 检查商品是否有订单
     * @param commodityId 商品ID
     * @param repository 订单Repository
     * @throws BusinessException 如果商品有订单
     */
    public static void requireNoOrders(String commodityId, OrderRepository repository) {
        List<Order> orders = repository.findByCommodityId(commodityId);
        if (!orders.isEmpty()) {
            throw new BusinessException("该商品已有订单，无法删除");
        }
    }
    
    /**
     * 检查字符串是否为空
     * @param value 字符串值
     * @param message 错误消息
     * @throws BusinessException 如果字符串为空
     */
    public static void requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
    }
}

