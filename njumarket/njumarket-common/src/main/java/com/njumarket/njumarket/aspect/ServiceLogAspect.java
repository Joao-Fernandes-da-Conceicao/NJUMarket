package com.njumarket.njumarket.aspect;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Service层方法日志切面
 * 统一记录Service层方法的执行日志，减少重复代码
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class ServiceLogAspect {

    /**
     * 切点：拦截所有Service实现类的public方法
     * 适配微服务架构：匹配所有服务的 service.impl 包
     */
    @Pointcut("execution(public * com.njumarket..service.impl.*.*(..))")
    public void serviceMethod() {
    }

    /**
     * 排除定时任务方法
     */
    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void scheduledMethod() {
    }

    /**
     * 组合切点：Service方法但不包括定时任务
     */
    @Pointcut("serviceMethod() && !scheduledMethod()")
    public void serviceMethodExcludeScheduled() {
    }

    /**
     * 环绕通知：记录方法执行日志
     */
    @Around("serviceMethodExcludeScheduled()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        Object[] args = joinPoint.getArgs();

        // 获取中文操作名称
        String operationName = getOperationName(methodName);

        // 记录方法开始
        String paramsStr = formatParams(args);
        log.info("{}开始 - className={}, method={}, params={}", 
                operationName, className, methodName, paramsStr);

        try {
            // 执行方法
            Object result = joinPoint.proceed();
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录方法成功
            String resultStr = formatResult(result);
            log.info("{}成功 - className={}, method={}, result={}, executionTime={}ms", 
                    operationName, className, methodName, resultStr, executionTime);
            
            return result;
            
        } catch (BusinessException e) {
            // 业务异常：WARN级别
            long executionTime = System.currentTimeMillis() - startTime;
            log.warn("{}失败（业务异常） - className={}, method={}, error={}, executionTime={}ms", 
                    operationName, className, methodName, e.getMessage(), executionTime);
            throw e;
            
        } catch (Exception e) {
            // 系统异常：ERROR级别
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("{}失败（系统异常） - className={}, method={}, error={}, executionTime={}ms", 
                    operationName, className, methodName, e.getMessage(), executionTime, e);
            throw e;
        }
    }

    /**
     * 根据方法名获取中文操作名称
     */
    private String getOperationName(String methodName) {
        // 方法名到中文操作名称的映射
        Map<String, String> operationMap = new HashMap<>();
        
        // 订单相关
        operationMap.put("createOrder", "创建订单");
        operationMap.put("payOrder", "支付订单");
        operationMap.put("confirmOrder", "确认收货");
        operationMap.put("cancelOrder", "取消订单");
        operationMap.put("shipOrder", "发货");
        operationMap.put("requestRefund", "申请退款");
        operationMap.put("handleRefund", "处理退款");
        operationMap.put("requestReturn", "申请退货");
        operationMap.put("approveReturnRequest", "审批退货申请");
        operationMap.put("confirmReturnShipment", "确认退货发货");
        operationMap.put("completeReturn", "完成退货");
        operationMap.put("getBuyerOrders", "获取买家订单列表");
        operationMap.put("getSellerOrders", "获取卖家订单列表");
        operationMap.put("getOrderDetail", "获取订单详情");
        operationMap.put("getOrderById", "获取订单信息");
        operationMap.put("updateOrderFull", "更新订单");
        operationMap.put("deleteOrder", "删除订单");
        operationMap.put("listOrders", "获取订单列表");
        
        // 商品相关
        operationMap.put("publishCommodity", "发布商品");
        operationMap.put("createDraftCommodity", "创建草稿商品");
        operationMap.put("publishDraftCommodity", "发布草稿商品");
        operationMap.put("updateCommodity", "更新商品");
        operationMap.put("deleteCommodity", "删除商品");
        operationMap.put("shelfCommodity", "上架商品");
        operationMap.put("unshelfCommodity", "下架商品");
        operationMap.put("draftCommodity", "设为草稿");
        operationMap.put("republishCommodity", "重新上架商品");
        operationMap.put("getCommodityById", "获取商品信息");
        operationMap.put("getMyCommodities", "获取我的商品列表");
        operationMap.put("getMyCommodityDetail", "获取我的商品详情");
        operationMap.put("listCommodities", "获取商品列表");
        operationMap.put("updateCommodityFull", "更新商品");
        operationMap.put("updateCommodityStatus", "更新商品状态");
        operationMap.put("uploadImage", "上传图片");
        operationMap.put("uploadCommodityImage", "上传商品图片");
        
        // 消息相关
        operationMap.put("sendMessage", "发送消息");
        operationMap.put("getConversations", "获取对话列表");
        operationMap.put("getConversationById", "获取对话详情");
        operationMap.put("getMessages", "获取消息列表");
        operationMap.put("getMessageById", "获取消息详情");
        operationMap.put("listConversations", "获取会话列表");
        operationMap.put("listMessages", "获取消息列表");
        operationMap.put("updateConversationFull", "更新会话");
        operationMap.put("deleteConversation", "删除会话");
        operationMap.put("updateMessageFull", "更新消息");
        operationMap.put("deleteMessage", "删除消息");
        
        // 用户相关
        operationMap.put("login", "用户登录");
        operationMap.put("logout", "用户登出");
        operationMap.put("register", "用户注册");
        operationMap.put("getUserById", "获取用户信息");
        operationMap.put("updateUser", "更新用户信息");
        operationMap.put("updateUserFull", "完整更新用户");
        operationMap.put("updateUserStatus", "更新用户状态");
        operationMap.put("deleteUser", "删除用户");
        operationMap.put("listUsers", "获取用户列表");
        operationMap.put("getUserProfile", "获取用户档案");
        operationMap.put("updateUserProfile", "更新用户档案");
        operationMap.put("uploadAvatar", "上传头像");
        operationMap.put("deleteAvatar", "删除头像");
        
        // 管理员相关
        operationMap.put("adminLogin", "管理员登录");
        operationMap.put("adminLogout", "管理员登出");
        operationMap.put("getCurrentAdmin", "获取当前管理员");
        operationMap.put("createAdmin", "创建管理员");
        operationMap.put("updateAdmin", "更新管理员");
        operationMap.put("deleteAdmin", "删除管理员");
        operationMap.put("getAdminList", "获取管理员列表");
        operationMap.put("getAdminById", "获取管理员信息");
        operationMap.put("updateAdminFull", "完整更新管理员");
        operationMap.put("resetPassword", "重置密码");
        operationMap.put("changePassword", "修改密码");
        
        // 如果映射中存在，返回中文名称
        if (operationMap.containsKey(methodName)) {
            return operationMap.get(methodName);
        }
        
        // 否则根据方法名推断
        if (methodName.startsWith("get") || methodName.startsWith("list") || methodName.startsWith("query")) {
            return "查询" + extractEntityName(methodName);
        } else if (methodName.startsWith("create") || methodName.startsWith("add")) {
            return "创建" + extractEntityName(methodName);
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "更新" + extractEntityName(methodName);
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "删除" + extractEntityName(methodName);
        } else if (methodName.startsWith("upload")) {
            return "上传" + extractEntityName(methodName);
        }
        
        // 默认返回方法名
        return methodName;
    }

    /**
     * 从方法名中提取实体名称
     */
    private String extractEntityName(String methodName) {
        // 移除常见前缀
        String name = methodName;
        if (name.startsWith("get") || name.startsWith("list") || name.startsWith("query") || 
            name.startsWith("create") || name.startsWith("add") || name.startsWith("update") || 
            name.startsWith("modify") || name.startsWith("delete") || name.startsWith("remove") ||
            name.startsWith("upload")) {
            name = name.substring(3);
        }
        
        // 转换为中文（简单映射）
        Map<String, String> entityMap = new HashMap<>();
        entityMap.put("Order", "订单");
        entityMap.put("Commodity", "商品");
        entityMap.put("Message", "消息");
        entityMap.put("Conversation", "会话");
        entityMap.put("User", "用户");
        entityMap.put("Admin", "管理员");
        entityMap.put("Avatar", "头像");
        entityMap.put("Image", "图片");
        
        for (Map.Entry<String, String> entry : entityMap.entrySet()) {
            if (name.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return "";
    }

    /**
     * 格式化参数显示
     */
    private String formatParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "无参数";
        }
        
        if (args.length == 1) {
            return formatSingleParam(args[0]);
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatSingleParam(args[i]));
        }
        return sb.toString();
    }

    /**
     * 格式化单个参数
     */
    private String formatSingleParam(Object arg) {
        if (arg == null) {
            return "null";
        }
        
        // 简单类型直接显示
        if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
            return String.valueOf(arg);
        }
        
        // DTO对象：只显示类名
        String className = arg.getClass().getSimpleName();
        if (className.endsWith("DTO") || className.endsWith("Request") || className.endsWith("Form")) {
            return className + "(...)";
        }
        
        // 集合类型：显示类型和大小
        if (arg instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) arg;
            return className + "(size=" + collection.size() + ")";
        }
        
        // Map类型：显示类型和大小
        if (arg instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) arg;
            return className + "(size=" + map.size() + ")";
        }
        
        // 其他复杂对象：只显示类名
        return className + "(...)";
    }

    /**
     * 格式化返回值显示
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        // Result类型：显示成功状态和错误信息
        if (result instanceof Result) {
            Result resultObj = (Result) result;
            if (resultObj.getSuccess()) {
                Object data = resultObj.getData();
                if (data == null) {
                    return "成功";
                }
                
                // 分页对象：显示总数
                if (data instanceof Page) {
                    Page<?> page = (Page<?>) data;
                    return "成功，总数=" + page.getTotalElements();
                }
                
                // 集合类型：显示大小
                if (data instanceof java.util.Collection) {
                    java.util.Collection<?> collection = (java.util.Collection<?>) data;
                    return "成功，数量=" + collection.size();
                }
                
                return "成功，data=" + data.getClass().getSimpleName();
            } else {
                return "失败，error=" + resultObj.getErrorMsg();
            }
        }
        
        // 分页对象：显示总数
        if (result instanceof Page) {
            Page<?> page = (Page<?>) result;
            return "总数=" + page.getTotalElements();
        }
        
        // 集合类型：显示大小
        if (result instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) result;
            return "数量=" + collection.size();
        }
        
        // 其他类型：显示类名
        return result.getClass().getSimpleName();
    }
}

