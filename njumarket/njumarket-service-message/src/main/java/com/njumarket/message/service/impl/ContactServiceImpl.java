package com.njumarket.message.service.impl;

import com.njumarket.message.dto.ConversationDTO;
import com.njumarket.message.dto.MessageDTO;
import com.njumarket.message.dto.SendMessageRequest;
import com.njumarket.njumarket.dto.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.message.entity.Conversation; // Conversation 实体从 message-service 导入
import com.njumarket.message.entity.Message; // Message 实体从 message-service 导入
import com.njumarket.message.entity.User; // User 实体（Message Service专用）
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.message.repository.ConversationRepository;
import com.njumarket.message.repository.MessageRepository;
import com.njumarket.message.service.ContactService;
import com.njumarket.message.client.AuthClient;
import com.njumarket.message.client.CommodityClient;
import com.njumarket.message.client.OrderClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    
    // ✅ 使用Feign Client调用其他服务
    private final AuthClient authClient;
    private final CommodityClient commodityClient;
    private final OrderClient orderClient;
    private final ObjectMapper objectMapper;
    
    private final SimpMessagingTemplate messagingTemplate;
    
    private final com.njumarket.message.service.WebSocketRetryService webSocketRetryService;
    
    @Override
    public Result sendMessage(String userId, SendMessageRequest request) {
        try {
            // ✅ 鉴权检查：验证当前登录用户（使用 SecurityUtils）
            Object userObj = com.njumarket.njumarket.utils.SecurityUtils.requireCurrentUser();
            User currentUser = (User) userObj;
            
            // ✅ 鉴权检查：验证传入的userId与当前登录用户匹配（防止用户冒充）
            if (!currentUser.getUserId().equals(userId)) {
                throw new BusinessException("无权操作：用户ID不匹配");
            }
            
            // ✅ 鉴权检查：验证用户状态是否为ACTIVE
            if (!"ACTIVE".equals(currentUser.getAccountStatus())) {
                throw new BusinessException("账户已被禁用，无法发送消息");
            }
            
            // ✅ 验证接收者是否存在（使用Feign Client）
            Result receiverResult = authClient.getUserById(request.getReceiverId());
            if (!receiverResult.getSuccess() || receiverResult.getData() == null) {
                throw new BusinessException("接收者不存在");
            }
            
            // ✅ 鉴权检查：验证接收者状态是否为ACTIVE
            // 使用ObjectMapper正确转换类型（避免ClassCastException）
            UserInternalDTO receiverDTO;
            try {
                receiverDTO = objectMapper.convertValue(
                    receiverResult.getData(),
                    new TypeReference<UserInternalDTO>() {}
                );
            } catch (Exception e) {
                log.error("转换User失败: receiverId={}, error={}", request.getReceiverId(), e.getMessage(), e);
                throw new BusinessException("接收者信息解析失败");
            }
            
            // 转换为User实体
            User receiver = new User();
            receiver.setUserId(receiverDTO.getUserId());
            receiver.setUsername(receiverDTO.getUsername());
            receiver.setPrimaryPhone(receiverDTO.getPrimaryPhone());
            receiver.setAccountStatus(receiverDTO.getAccountStatus());
            receiver.setRegisterTime(receiverDTO.getRegisterTime());
            
            if (!"ACTIVE".equals(receiver.getAccountStatus())) {
                throw new BusinessException("接收者账户已被禁用");
            }
            
            // ✅ 鉴权检查：防止用户向自己发送消息（可选，根据业务需求）
            if (userId.equals(request.getReceiverId())) {
                throw new BusinessException("不能向自己发送消息");
            }
            
            // 获取或创建对话
            Conversation conversation;
            if (request.getConversationId() != null) {
                Optional<Conversation> convOpt = conversationRepository.findById(request.getConversationId());
                if (!convOpt.isPresent()) {
                    throw new BusinessException("对话不存在");
                }
                conversation = convOpt.get();
                
                // 验证用户是否属于这个对话
                if (!conversation.involvesUser(userId)) {
                    throw new BusinessException("无权访问此对话");
                }
                
                // ✅ 如果接收方（B）删除了会话（不可见），自动恢复接收方的可见性
                // A向B发消息时，如果B的可见性为false，则恢复为true
                boolean visibilityRestored = false;
                if (!conversation.getVisibilityForUser(request.getReceiverId())) {
                    conversation.restoreVisibilityForUser(request.getReceiverId());
                    visibilityRestored = true;
                    log.debug("恢复接收方会话可见性: conversationId={}, receiverId={}", 
                            conversation.getConversationId(), request.getReceiverId());
                }
                
                // ✅ 如果恢复了可见性，需要推送完整会话信息（用于前端自动添加到会话列表）
                if (visibilityRestored) {
                    // 批量查询用户信息（避免N+1查询）
                    Set<String> userIds = new HashSet<>();
                    userIds.add(conversation.getUserId1());
                    userIds.add(conversation.getUserId2());
                    
                    Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
                    Map<String, User> userMap = new HashMap<>();
                    if (!userIds.isEmpty()) {
                        // ✅ 使用Feign Client批量查询用户档案
                        Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
                        if (profileResult.getSuccess() && profileResult.getData() != null) {
                            try {
                                List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                                    profileResult.getData(),
                                    new TypeReference<List<UserProfileInternalDTO>>() {}
                                );
                                profileMap = profiles.stream()
                                        .collect(Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                            } catch (Exception e) {
                                log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                            }
                        }
                        
                        // ✅ 使用Feign Client批量查询用户
                        Result userResult = authClient.getUsersByIds(new ArrayList<>(userIds));
                        if (userResult.getSuccess() && userResult.getData() != null) {
                            try {
                                // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
                                List<UserInternalDTO> userDTOs = objectMapper.convertValue(
                                    userResult.getData(),
                                    new TypeReference<List<UserInternalDTO>>() {}
                                );
                                // 转换为User实体
                                userMap = userDTOs.stream()
                                        .map(dto -> {
                                            User user = new User();
                                            user.setUserId(dto.getUserId());
                                            user.setUsername(dto.getUsername());
                                            user.setPrimaryPhone(dto.getPrimaryPhone());
                                            user.setAccountStatus(dto.getAccountStatus());
                                            user.setRegisterTime(dto.getRegisterTime());
                                            return user;
                                        })
                                        .collect(Collectors.toMap(User::getUserId, u -> u));
                            } catch (Exception e) {
                                log.error("转换User列表失败: {}", e.getMessage(), e);
                                userMap = new HashMap<>();
                            }
                        }
                    }
                    
                    // 转换为完整的会话DTO
                    ConversationDTO conversationDTO = convertConversationToDTOWithMap(
                            conversation, request.getReceiverId(), profileMap, userMap);
                    // ✅ 确保推送的会话不包含消息列表（用于会话列表，不需要消息详情）
                    conversationDTO.setMessages(null);
                    conversationDTO.setTotalMessages(null);
                    
                    // 推送会话恢复通知
                    Map<String, Object> conversationRestore = new java.util.HashMap<>();
                    conversationRestore.put("type", "CONVERSATION_VISIBILITY_RESTORED");
                    conversationRestore.put("conversation", conversationDTO);
                    conversationRestore.put("conversationId", conversation.getConversationId());
                    conversationRestore.put("timestamp", LocalDateTime.now().toString());
                    
                    webSocketRetryService.pushWithRetry(request.getReceiverId(), conversationRestore, "CONVERSATION_RESTORED");
                    log.debug("推送会话恢复通知: receiverId={}, conversationId={}", 
                            request.getReceiverId(), conversation.getConversationId());
                }
            } else {
                // 创建新对话（基于用户对，确保唯一性）
                Optional<Conversation> existingConv = conversationRepository.findByUserPairActive(userId, request.getReceiverId());
                if (existingConv.isPresent()) {
                    conversation = existingConv.get();
                    // ✅ 如果接收方（B）删除了会话（不可见），自动恢复接收方的可见性
                    boolean visibilityRestored = false;
                    if (!conversation.getVisibilityForUser(request.getReceiverId())) {
                        conversation.restoreVisibilityForUser(request.getReceiverId());
                        visibilityRestored = true;
                    }
                    
                    // ✅ 如果恢复了可见性，需要推送完整会话信息（用于前端自动添加到会话列表）
                    if (visibilityRestored) {
                        // 批量查询用户信息（避免N+1查询）
                        Set<String> userIds = new HashSet<>();
                        userIds.add(conversation.getUserId1());
                        userIds.add(conversation.getUserId2());
                        
                        Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
                        Map<String, User> userMap = new HashMap<>();
                        if (!userIds.isEmpty()) {
                            // ✅ 使用Feign Client批量查询用户档案
                            Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
                            if (profileResult.getSuccess() && profileResult.getData() != null) {
                                try {
                                    List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                                        profileResult.getData(),
                                        new TypeReference<List<UserProfileInternalDTO>>() {}
                                    );
                                    profileMap = profiles.stream()
                                            .collect(Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                                } catch (Exception e) {
                                    log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                                }
                            }
                            
                            // ✅ 使用Feign Client批量查询用户
                            Result userResult = authClient.getUsersByIds(new ArrayList<>(userIds));
                            if (userResult.getSuccess() && userResult.getData() != null) {
                                try {
                                    // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
                                    List<UserInternalDTO> userDTOs = objectMapper.convertValue(
                                        userResult.getData(),
                                        new TypeReference<List<UserInternalDTO>>() {}
                                    );
                                    // 转换为User实体
                                    userMap = userDTOs.stream()
                                            .map(dto -> {
                                                User user = new User();
                                                user.setUserId(dto.getUserId());
                                                user.setUsername(dto.getUsername());
                                                user.setPrimaryPhone(dto.getPrimaryPhone());
                                                user.setAccountStatus(dto.getAccountStatus());
                                                user.setRegisterTime(dto.getRegisterTime());
                                                return user;
                                            })
                                            .collect(Collectors.toMap(User::getUserId, u -> u));
                                } catch (Exception e) {
                                    log.error("转换User列表失败: {}", e.getMessage(), e);
                                }
                            }
                        }
                        
                        // 转换为完整的会话DTO
                        ConversationDTO conversationDTO = convertConversationToDTOWithMap(
                                conversation, request.getReceiverId(), profileMap, userMap);
                        // ✅ 确保推送的会话不包含消息列表（用于会话列表，不需要消息详情）
                        conversationDTO.setMessages(null);
                        conversationDTO.setTotalMessages(null);
                        
                        // 推送会话恢复通知
                        Map<String, Object> conversationRestore = new java.util.HashMap<>();
                        conversationRestore.put("type", "CONVERSATION_VISIBILITY_RESTORED");
                        conversationRestore.put("conversation", conversationDTO);
                        conversationRestore.put("conversationId", conversation.getConversationId());
                        conversationRestore.put("timestamp", LocalDateTime.now().toString());
                        
                        webSocketRetryService.pushWithRetry(request.getReceiverId(), conversationRestore, "CONVERSATION_RESTORED");
                        log.debug("推送会话恢复通知: receiverId={}, conversationId={}", 
                                request.getReceiverId(), conversation.getConversationId());
                    }
                } else {
                    conversation = new Conversation();
                    conversation.setUserPair(userId, request.getReceiverId());
                    conversation.setStatus("ACTIVE");
                    // 新会话默认双方都可见
                    conversation.setUser1Visibility(true);
                    conversation.setUser2Visibility(true);
                    conversationRepository.save(conversation);
                }
            }
            
            // 创建消息
            Message message = new Message();
            message.setConversationId(conversation.getConversationId());
            message.setSenderId(userId);
            message.setReceiverId(request.getReceiverId());
            message.setMessageType(request.getMessageType() != null ? request.getMessageType() : "TEXT");
            message.setContent(request.getContent());
            message.setImageUrl(request.getImageUrl());
            
            // 处理商品卡片和订单卡片
            if ("COMMODITY_CARD".equals(request.getMessageType()) && request.getCommodityId() != null) {
                // ✅ 商品卡片鉴权：验证商品是否存在（使用Feign Client）
                Result commodityResult = commodityClient.getCommodityById(request.getCommodityId());
                if (!commodityResult.getSuccess() || commodityResult.getData() == null) {
                    throw new BusinessException("商品不存在");
                }
                // ⚠️ 注意：不再转换为Commodity实体，直接使用DTO数据
                @SuppressWarnings("unchecked")
                Map<String, Object> commodityData = (Map<String, Object>) commodityResult.getData();
                String commodityId = (String) commodityData.get("commodityId");
                String sellerId = (String) commodityData.get("sellerId");
                String title = (String) commodityData.get("title");
                String commodityStatus = (String) commodityData.get("commodityStatus");
                
                // ✅ 商品卡片鉴权：验证商品的卖家必须是对话双方之一
                // 获取对话的对方用户
                String otherUserId = conversation.getOtherUserId(userId);
                if (otherUserId == null) {
                    throw new BusinessException("无法确定对话对方用户");
                }
                
                // ✅ 验证商品的卖家必须匹配对话的双方用户之一
                // 场景1：卖家（sellerId）向买家发送商品卡片
                // 场景2：买家咨询卖家的商品
                boolean sellerIsSender = sellerId != null && sellerId.equals(userId);
                boolean sellerIsReceiver = sellerId != null && sellerId.equals(otherUserId);
                
                // 商品卖家必须是对话双方之一
                if (!sellerIsSender && !sellerIsReceiver) {
                    throw new BusinessException("无权发送此商品卡片：商品不属于当前对话双方");
                }
                
                // ✅ 商品卡片鉴权：验证商品状态（可选，根据业务需求）
                // 已下架或草稿状态的商品可能不允许发送
                if ("OFF_SHELF".equals(commodityStatus) || "DRAFT".equals(commodityStatus)) {
                    throw new BusinessException("商品状态不允许发送：商品已下架或为草稿状态");
                }
                
                // 设置商品卡片相关字段
                message.setCommodityId(request.getCommodityId());
                message.setMessageType("COMMODITY_CARD");
                if (message.getContent() == null || message.getContent().isEmpty()) {
                    message.setContent("商品：" + (title != null ? title : "未知商品"));
                }
            }
            
            if ("ORDER_CARD".equals(request.getMessageType()) && request.getOrderId() != null) {
                // ✅ 验证订单是否属于对话双方（使用Feign Client）
                Result orderResult = orderClient.getOrderById(request.getOrderId());
                if (!orderResult.getSuccess() || orderResult.getData() == null) {
                    throw new BusinessException("订单不存在");
                }
                // ⚠️ 注意：不再转换为Order实体，直接使用DTO数据
                @SuppressWarnings("unchecked")
                Map<String, Object> orderData = (Map<String, Object>) orderResult.getData();
                String orderId = (String) orderData.get("orderId");
                String buyerId = (String) orderData.get("buyerId");
                String sellerId = (String) orderData.get("sellerId");
                String otherUserId = conversation.getOtherUserId(userId);
                if (otherUserId == null) {
                    throw new BusinessException("无法确定对话对方用户");
                }
                
                // 验证订单的买卖双方必须匹配对话的双方用户
                boolean buyerMatches = buyerId != null && (buyerId.equals(userId) || buyerId.equals(otherUserId));
                boolean sellerMatches = sellerId != null && (sellerId.equals(userId) || sellerId.equals(otherUserId));
                if (!buyerMatches || !sellerMatches) {
                    throw new BusinessException("无权发送此订单卡片：订单不属于当前对话双方");
                }
                
                // 设置订单卡片相关字段
                message.setOrderId(request.getOrderId());
                message.setMessageType("ORDER_CARD");
                if (message.getContent() == null || message.getContent().isEmpty()) {
                    message.setContent("订单：" + (orderId != null ? orderId : request.getOrderId()));
                }
            }
            
            // 如果发送的是普通文本消息但提供了commodityId或orderId，也设置字段（用于后续扩展）
            if (request.getCommodityId() != null && !"COMMODITY_CARD".equals(request.getMessageType())) {
                message.setCommodityId(request.getCommodityId());
            }
            if (request.getOrderId() != null && !"ORDER_CARD".equals(request.getMessageType())) {
                message.setOrderId(request.getOrderId());
            }
            
            message.setIsRead(false);
            // deletedBySender 和 deletedByReceiver 默认值为 false，不需要设置
            messageRepository.save(message);
            
            // ✅ 更新对话最后消息
            LocalDateTime now = LocalDateTime.now();
            String messageContent = request.getContent();
            
            // 更新管理端字段（不过滤，显示真实最新消息）
            conversation.setLastMessageContent(messageContent);
            conversation.setLastMessageTime(now);
            
            // ✅ 更新用户级别的最后消息字段（新消息对发送方和接收方都可见）
            // 发送方（userId）的最后消息
            conversation.setLastMessageForUser(userId, messageContent, now);
            // 接收方（request.getReceiverId()）的最后消息
            conversation.setLastMessageForUser(request.getReceiverId(), messageContent, now);
            
            conversation.incrementUnreadForUser(request.getReceiverId());
            conversationRepository.save(conversation);
            
            // 转换为DTO返回
            MessageDTO messageDTO = convertMessageToDTO(message, userId);
            
            // ✅ WebSocket 推送：实时推送新消息给接收方（带重试机制）
            String receiverId = request.getReceiverId();
            
            // 使用重试服务推送消息（自动处理离线用户和重试逻辑）
            webSocketRetryService.pushWithRetry(receiverId, messageDTO, "MESSAGE_NEW");
            log.debug("WebSocket push attempted (with retry): receiverId={}, messageId={}", 
                        receiverId, messageDTO.getMessageId());
            
            // ✅ 统一推送未读数更新事件（新消息 = 增加未读）
            Integer totalUnreadCount = conversationRepository.getTotalUnreadCount(receiverId);
            if (totalUnreadCount == null) {
                totalUnreadCount = 0;
            }
            
            // ✅ 获取该对话的未读数（用于侧边栏显示单个对话未读数）
            Integer conversationUnreadCount = conversation.getUnreadCountForUser(receiverId);
            if (conversationUnreadCount == null) {
                conversationUnreadCount = 0;
            }
            
            Map<String, Object> unreadCountUpdate = new java.util.HashMap<>();
            unreadCountUpdate.put("type", "UNREAD_COUNT_UPDATE");
            unreadCountUpdate.put("unreadCount", totalUnreadCount); // 总未读数（用于顶部栏）
            unreadCountUpdate.put("conversationId", conversation.getConversationId());
            unreadCountUpdate.put("conversationUnreadCount", conversationUnreadCount); // 单个对话未读数（用于侧边栏）
            unreadCountUpdate.put("timestamp", LocalDateTime.now().toString());
            
            // 使用重试服务推送未读数更新
            webSocketRetryService.pushWithRetry(receiverId, unreadCountUpdate, "UNREAD_COUNT_UPDATE");
            
            return Result.ok("消息发送成功", messageDTO);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("发送消息失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result getConversations(String userId, int page, int size) {
        try {
            log.info("开始查询对话列表: userId={}, page={}, size={}", userId, page, size);
            
            // 调试：查询所有包含该用户的对话（不过滤可见性），用于诊断问题
            List<Conversation> allUserConversations = conversationRepository.findByUserIdAndStatus(userId, "ACTIVE");
            log.info("调试查询（不过滤可见性）: userId={}, 总数={}", userId, allUserConversations.size());
            for (Conversation conv : allUserConversations) {
                log.info("调试对话详情: conversationId={}, userId1={}, userId2={}, user1Visibility={}, user2Visibility={}, status={}", 
                    conv.getConversationId(), conv.getUserId1(), conv.getUserId2(), 
                    conv.getUser1Visibility(), conv.getUser2Visibility(), conv.getStatus());
            }
            
            // ✅ 优化方案3：分别查询userId1和userId2的对话，避免OR条件导致的索引失效
            // 查询用户作为userId1的对话（按user1LastMessageTime排序）
            List<Conversation> conversationsAsUser1 = conversationRepository
                    .findByUserId1AndStatusOrderByUser1LastMessageTime(userId, "ACTIVE");
            log.info("查询用户作为userId1的对话（过滤可见性）: userId={}, 数量={}", userId, conversationsAsUser1.size());
            
            // 查询用户作为userId2的对话（按user2LastMessageTime排序）
            List<Conversation> conversationsAsUser2 = conversationRepository
                    .findByUserId2AndStatusOrderByUser2LastMessageTime(userId, "ACTIVE");
            log.info("查询用户作为userId2的对话（过滤可见性）: userId={}, 数量={}", userId, conversationsAsUser2.size());
            
            // 合并两个列表（使用Map去重，避免重复对话）
            Map<String, Conversation> conversationMap = new LinkedHashMap<>();
            
            // 先添加userId1的对话（已按时间排序）
            for (Conversation conv : conversationsAsUser1) {
                conversationMap.put(conv.getConversationId(), conv);
            }
            
            // 再添加userId2的对话（已按时间排序），如果有重复会覆盖，但时间顺序已保证
            for (Conversation conv : conversationsAsUser2) {
                conversationMap.put(conv.getConversationId(), conv);
            }
            
            // 转换为List并重新排序（合并后的最终排序）
            // 因为两个列表都已按各自的时间字段排序，需要统一按用户级别时间排序
            List<Conversation> allConversations = new ArrayList<>(conversationMap.values());
            log.info("合并后的对话总数: userId={}, 总数={}", userId, allConversations.size());
            
            allConversations.sort((c1, c2) -> {
                        // 按用户对应的最后消息时间降序排序
                        LocalDateTime time1 = c1.getLastMessageTimeForUser(userId);
                        LocalDateTime time2 = c2.getLastMessageTimeForUser(userId);
                        if (time1 == null && time2 == null) return 0;
                        if (time1 == null) return 1; // null 排在后面
                        if (time2 == null) return -1;
                        return time2.compareTo(time1); // 降序
            });
            
            // 手动分页
            int total = allConversations.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);
            List<Conversation> pagedConversations = fromIndex < total 
                    ? allConversations.subList(fromIndex, toIndex) 
                    : new ArrayList<>();
            log.info("分页后的对话数量: userId={}, total={}, page={}, size={}, pagedCount={}", 
                userId, total, page, size, pagedConversations.size());
            
            // ✅ 优化：批量查询所有相关的 UserProfile（避免 N+1 查询）
            // 收集所有相关的用户ID（去重）
            Set<String> userIds = new HashSet<>();
            Set<String> userIdsForUserCheck = new HashSet<>(); // 用于检查用户是否已注销
            for (Conversation conv : pagedConversations) {
                userIds.add(conv.getUserId1());
                userIds.add(conv.getUserId2());
                userIdsForUserCheck.add(conv.getUserId1());
                userIdsForUserCheck.add(conv.getUserId2());
            }
            
            // ✅ 优化：批量查询边界处理 - 只在有用户ID时才查询（使用Feign Client）
            Map<String, UserProfileInternalDTO> profileMapTemp = new HashMap<>();
            if (!userIds.isEmpty()) {
                Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
                if (profileResult.getSuccess() && profileResult.getData() != null) {
                    try {
                        List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                            profileResult.getData(),
                            new TypeReference<List<UserProfileInternalDTO>>() {}
                        );
                        profileMapTemp = profiles.stream()
                                .collect(Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                    } catch (Exception e) {
                        log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                    }
                }
            }
            final Map<String, UserProfileInternalDTO> profileMap = profileMapTemp;
            
            // ✅ 优化：批量查询 User（用于检查用户是否已注销）（使用Feign Client）
            Map<String, User> userMapTemp = new HashMap<>();
            if (!userIdsForUserCheck.isEmpty()) {
                Result userResult = authClient.getUsersByIds(new ArrayList<>(userIdsForUserCheck));
                if (userResult.getSuccess() && userResult.getData() != null) {
                    try {
                        // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
                        List<UserInternalDTO> userDTOs = objectMapper.convertValue(
                            userResult.getData(),
                            new TypeReference<List<UserInternalDTO>>() {}
                        );
                        // 转换为User实体
                        userMapTemp = userDTOs.stream()
                                .map(dto -> {
                                    User user = new User();
                                    user.setUserId(dto.getUserId());
                                    user.setUsername(dto.getUsername());
                                    user.setPrimaryPhone(dto.getPrimaryPhone());
                                    user.setAccountStatus(dto.getAccountStatus());
                                    user.setRegisterTime(dto.getRegisterTime());
                                    return user;
                                })
                                .collect(Collectors.toMap(User::getUserId, u -> u));
                    } catch (Exception e) {
                        log.error("转换User列表失败: userId={}, error={}", userId, e.getMessage(), e);
                    }
                }
            }
            final Map<String, User> userMap = userMapTemp;
            
            // 转换为 DTO（使用批量查询的 Map）
            log.info("开始转换对话为DTO: userId={}, 对话数量={}, profileMap大小={}, userMap大小={}", 
                userId, pagedConversations.size(), profileMap.size(), userMap.size());
            
            List<ConversationDTO> dtoList = new ArrayList<>();
            for (int i = 0; i < pagedConversations.size(); i++) {
                Conversation conversation = pagedConversations.get(i);
                try {
                    log.debug("转换对话 {}: conversationId={}, userId1={}, userId2={}", 
                        i + 1, conversation.getConversationId(), 
                        conversation.getUserId1(), conversation.getUserId2());
                    
                    ConversationDTO dto = convertConversationToDTOWithMap(conversation, userId, profileMap, userMap);
                    dtoList.add(dto);
                    
                    log.debug("对话 {} 转换成功: conversationId={}, otherUserId={}", 
                        i + 1, dto.getConversationId(), dto.getOtherUserId());
                } catch (Exception e) {
                    log.error("转换对话失败: conversationId={}, userId1={}, userId2={}, error={}", 
                        conversation.getConversationId(), 
                        conversation.getUserId1(), 
                        conversation.getUserId2(), 
                        e.getMessage(), e);
                    throw new BusinessException("转换对话失败: " + e.getMessage(), e);
                }
            }
            
            log.info("对话转换完成: userId={}, 成功转换数量={}", userId, dtoList.size());
            
            // 构造分页结果
            Pageable pageable = PageRequest.of(page, size);
            Page<ConversationDTO> dtoPage = new PageImpl<>(dtoList, pageable, total);
            
            return Result.ok("获取对话列表成功", dtoPage);
        } catch (BusinessException e) {
            log.error("获取对话列表业务异常: userId={}, error={}", userId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("获取对话列表系统异常: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException("获取对话列表失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result getConversationDetail(String userId, String conversationId, int page, int size) {
        try {
            log.info("开始获取对话详情: userId={}, conversationId={}, page={}, size={}", 
                userId, conversationId, page, size);
            
            // 查询对话
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                log.warn("对话不存在: conversationId={}", conversationId);
                throw new BusinessException("对话不存在");
            }
            
            Conversation conversation = convOpt.get();
            log.debug("查询到对话: conversationId={}, userId1={}, userId2={}", 
                conversation.getConversationId(), conversation.getUserId1(), conversation.getUserId2());
            
            // 验证权限（使用 involvesUser 方法，基于 user_id_1 和 user_id_2）
            if (!conversation.involvesUser(userId)) {
                log.warn("无权访问对话: userId={}, conversationId={}", userId, conversationId);
                throw new BusinessException("无权访问此对话");
            }
            
            // 查询消息列表
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> messagesPage = messageRepository.findByConversationId(conversationId, pageable);
            log.info("查询到消息: conversationId={}, 总数={}, 当前页数量={}", 
                conversationId, messagesPage.getTotalElements(), messagesPage.getContent().size());
            
            // ✅ 优化：批量查询两个用户的 UserProfile（只需要2次查询）
            // 在一个对话中，消息的发送者只有两种可能：当前用户或对方用户
            Set<String> userIds = new HashSet<>();
            userIds.add(conversation.getUserId1());
            userIds.add(conversation.getUserId2());
            
            // ✅ 使用Feign Client批量查询用户档案
            Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
                if (profileResult.getSuccess() && profileResult.getData() != null) {
                    try {
                        List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                            profileResult.getData(),
                            new TypeReference<List<UserProfileInternalDTO>>() {}
                        );
                        profileMap = profiles.stream()
                                .collect(Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                        log.debug("批量查询用户档案成功: userIds={}, profileMap大小={}", userIds, profileMap.size());
                    } catch (Exception e) {
                        log.error("转换UserProfileInternalDTO列表失败: conversationId={}, error={}", 
                            conversationId, e.getMessage(), e);
                        profileMap = new HashMap<>();
                    }
                } else {
                    log.warn("批量查询用户档案失败: conversationId={}, success={}", 
                        conversationId, profileResult.getSuccess());
                }
            }
            
            // 转换对话和消息为DTO，并过滤当前用户已删除的消息
            log.debug("开始转换对话为DTO: conversationId={}", conversationId);
            ConversationDTO dto;
            try {
                dto = convertConversationToDTO(conversation, userId);
                log.debug("对话转换成功: conversationId={}", conversationId);
            } catch (Exception e) {
                log.error("转换对话失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
                throw new BusinessException("转换对话失败: " + e.getMessage(), e);
            }
            
            log.debug("开始转换消息为DTO: conversationId={}, 消息数量={}", 
                conversationId, messagesPage.getContent().size());
            List<MessageDTO> messageDTOs = new ArrayList<>();
            for (int i = 0; i < messagesPage.getContent().size(); i++) {
                Message message = messagesPage.getContent().get(i);
                try {
                    // 过滤：如果当前用户已删除此消息，则不显示
                    boolean isDeletedByCurrentUser = false;
                    if (userId.equals(message.getSenderId()) && Boolean.TRUE.equals(message.getDeletedBySender())) {
                        // 当前用户是发送方，且发送方已删除
                        isDeletedByCurrentUser = true;
                    } else if (userId.equals(message.getReceiverId()) && Boolean.TRUE.equals(message.getDeletedByReceiver())) {
                        // 当前用户是接收方，且接收方已删除
                        isDeletedByCurrentUser = true;
                    }
                    
                    if (!isDeletedByCurrentUser) {
                        // ✅ 使用批量查询的 Map，不再单独查询
                        MessageDTO messageDTO = convertMessageToDTOWithMap(message, userId, profileMap);
                        messageDTOs.add(messageDTO);
                    }
                } catch (Exception e) {
                    log.error("转换消息失败: messageId={}, conversationId={}, error={}", 
                        message.getMessageId(), conversationId, e.getMessage(), e);
                    // 继续处理其他消息，不中断整个流程
                }
            }
            
            log.info("消息转换完成: conversationId={}, 原始消息数={}, 转换后消息数={}", 
                conversationId, messagesPage.getContent().size(), messageDTOs.size());
            
            dto.setMessages(messageDTOs);
            dto.setTotalMessages((int) messagesPage.getTotalElements()); // 使用总消息数，而不是过滤后的数量
            
            log.info("获取对话详情成功: conversationId={}, 消息数量={}", conversationId, messageDTOs.size());
            return Result.ok("获取对话详情成功", dto);
        } catch (BusinessException e) {
            log.error("获取对话详情业务异常: userId={}, conversationId={}, error={}", 
                userId, conversationId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("获取对话详情系统异常: userId={}, conversationId={}, error={}", 
                userId, conversationId, e.getMessage(), e);
            throw new BusinessException("获取对话详情失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result getMessagesBefore(String userId, String conversationId, String beforeTime, int size) {
        try {
            // 查询对话
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                throw new BusinessException("对话不存在");
            }
            
            Conversation conversation = convOpt.get();
            
            // 验证权限（使用 involvesUser 方法，基于 user_id_1 和 user_id_2）
            if (!conversation.involvesUser(userId)) {
                throw new BusinessException("无权访问此对话");
            }
            
            // 解析时间参数
            LocalDateTime beforeDateTime;
            try {
                beforeDateTime = LocalDateTime.parse(beforeTime);
            } catch (Exception e) {
                throw new BusinessException("时间参数格式错误");
            }
            
            // 查询指定时间之前的消息（按 DESC 排序，最新的在前）
            Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> messagesPage = messageRepository.findMessagesBefore(conversationId, beforeDateTime, pageable);
            
            // ✅ 优化：批量查询两个用户的 UserProfile（使用Feign Client）
            Set<String> userIds = new HashSet<>();
            userIds.add(conversation.getUserId1());
            userIds.add(conversation.getUserId2());
            
            Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
                if (profileResult.getSuccess() && profileResult.getData() != null) {
                    try {
                        List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                            profileResult.getData(),
                            new TypeReference<List<UserProfileInternalDTO>>() {}
                        );
                        profileMap = profiles.stream()
                                .collect(Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                    } catch (Exception e) {
                        log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                        profileMap = new HashMap<>();
                    }
                }
            }
            
            // 转换消息为DTO，并过滤当前用户已删除的消息
            List<MessageDTO> messageDTOs = new ArrayList<>();
            for (Message message : messagesPage.getContent()) {
                // 过滤：如果当前用户已删除此消息，则不显示
                boolean isDeletedByCurrentUser = false;
                if (userId.equals(message.getSenderId()) && Boolean.TRUE.equals(message.getDeletedBySender())) {
                    isDeletedByCurrentUser = true;
                } else if (userId.equals(message.getReceiverId()) && Boolean.TRUE.equals(message.getDeletedByReceiver())) {
                    isDeletedByCurrentUser = true;
                }
                
                if (!isDeletedByCurrentUser) {
                    messageDTOs.add(convertMessageToDTOWithMap(message, userId, profileMap));
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("messages", messageDTOs);
            result.put("hasMore", messagesPage.hasNext());
            
            return Result.ok("获取历史消息成功", result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("获取历史消息失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result getOrCreateConversation(String userId, String otherUserId) {
        try {
            // 查找现有活跃对话（基于用户对，确保唯一性）
            Optional<Conversation> existingConv = conversationRepository.findByUserPairActive(userId, otherUserId);
            
            Conversation conversation;
            if (existingConv.isPresent()) {
                conversation = existingConv.get();
            } else {
                // 创建新对话（使用标准化用户对）
                conversation = new Conversation();
                conversation.setUserPair(userId, otherUserId);
                conversation.setStatus("ACTIVE");
                conversationRepository.save(conversation);
            }
            
            ConversationDTO dto = convertConversationToDTO(conversation, userId);
            return Result.ok("获取对话成功", dto);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("获取或创建对话失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result markConversationAsRead(String userId, String conversationId) {
        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                throw new BusinessException("对话不存在");
            }
            
            Conversation conversation = convOpt.get();
            
            // 验证权限（使用 involvesUser 方法，基于 user_id_1 和 user_id_2）
            if (!conversation.involvesUser(userId)) {
                throw new BusinessException("无权访问此对话");
            }
            
            // 标记对话为已读
            conversation.markAsReadForUser(userId);
            conversationRepository.save(conversation);
            
            // ✅ 在标记为已读之前，先查询所有未读消息（用于向发送者推送已读通知）
            // 获取对话中的对方用户ID
            String otherUserId = conversation.getUserId1().equals(userId) 
                    ? conversation.getUserId2() 
                    : conversation.getUserId1();
            
            // 查询该对话中由对方发送给当前用户的未读消息
            List<Message> unreadMessages = messageRepository.findUnreadMessagesByConversationAndReceiver(
                    conversationId, userId);
            
            // 标记所有消息为已读
            LocalDateTime readTime = LocalDateTime.now();
            messageRepository.markMessagesAsRead(conversationId, userId, readTime);
            
            // ✅ 如果有未读消息，向发送者推送已读通知
            if (unreadMessages != null && !unreadMessages.isEmpty()) {
                try {
                    // 构建已读通知消息
                    Map<String, Object> readNotification = new java.util.HashMap<>();
                    readNotification.put("type", "MESSAGE_READ");
                    readNotification.put("conversationId", conversationId);
                    readNotification.put("readTime", readTime.toString());
                    
                    // 构建已读消息ID列表
                    List<String> readMessageIds = unreadMessages.stream()
                            .map(Message::getMessageId)
                            .collect(java.util.stream.Collectors.toList());
                    readNotification.put("messageIds", readMessageIds);
                    
                    // 向发送者推送已读通知（使用重试机制）
                    webSocketRetryService.pushWithRetry(otherUserId, readNotification, "MESSAGE_READ");
                } catch (Exception e) {
                    log.warn("推送已读通知失败: senderId={}, conversationId={}, error={}", 
                            otherUserId, conversationId, e.getMessage());
                    // WebSocket 推送失败不影响标记已读的成功返回
                }
            }
            
            // ✅ 推送未读数更新事件，让前端全局角标实时更新
            try {
                // 获取最新的未读消息总数（用于顶部栏）
                Integer totalUnreadCount = conversationRepository.getTotalUnreadCount(userId);
                if (totalUnreadCount == null) {
                    totalUnreadCount = 0;
                }
                
                // ✅ 获取该对话的未读数（标记已读后应该为0，用于侧边栏显示）
                Integer conversationUnreadCount = conversation.getUnreadCountForUser(userId);
                if (conversationUnreadCount == null) {
                    conversationUnreadCount = 0;
                }
                
                // 构建未读数更新消息
                Map<String, Object> unreadCountUpdate = new java.util.HashMap<>();
                unreadCountUpdate.put("type", "UNREAD_COUNT_UPDATE");
                unreadCountUpdate.put("unreadCount", totalUnreadCount); // 总未读数（用于顶部栏）
                unreadCountUpdate.put("conversationId", conversationId);
                unreadCountUpdate.put("conversationUnreadCount", conversationUnreadCount); // 单个对话未读数（用于侧边栏）
                unreadCountUpdate.put("timestamp", LocalDateTime.now().toString());
                
                // 使用重试服务推送未读数更新（带重试机制）
                webSocketRetryService.pushWithRetry(userId, unreadCountUpdate, "UNREAD_COUNT_UPDATE");
            } catch (Exception e) {
                log.warn("推送未读数更新失败: userId={}, error={}", userId, e.getMessage());
                // WebSocket 推送失败不影响标记已读的成功返回
            }
            
            return Result.ok("标记已读成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("标记已读失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result getUnreadCount(String userId) {
        try {
            Integer count = conversationRepository.getTotalUnreadCount(userId);
            return Result.ok("获取未读数成功", count != null ? count : 0);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("获取未读数失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result deleteConversation(String userId, String conversationId) {
        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                throw new BusinessException("对话不存在");
            }
            
            Conversation conversation = convOpt.get();
            if (!conversation.involvesUser(userId)) {
                throw new BusinessException("无权删除此对话");
            }
            
            // ✅ v1.3.x: 软删除对话（设置用户可见性为false，而不是修改status）
            // 这样只影响当前用户的对话列表，不影响对方用户
            conversation.setVisibilityForUser(userId, false);
            conversationRepository.save(conversation);
            
            return Result.ok("删除对话成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("删除对话失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result deleteMessage(String userId, String messageId) {
        try {
            Optional<Message> msgOpt = messageRepository.findById(messageId);
            if (!msgOpt.isPresent()) {
                throw new BusinessException("消息不存在");
            }
            
            Message message = msgOpt.get();
            
            // 验证权限：只能删除对话中的消息（发送方或接收方）
            if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
                throw new BusinessException("无权删除此消息");
            }
            
            // 获取对话信息
            Optional<Conversation> convOpt = conversationRepository.findById(message.getConversationId());
            if (!convOpt.isPresent()) {
                throw new BusinessException("对话不存在");
            }
            Conversation conversation = convOpt.get();
            
            // ✅ 保存原来的可见性状态（用于检测变化）
            Boolean oldDeletedBySender = message.getDeletedBySender();
            Boolean oldDeletedByReceiver = message.getDeletedByReceiver();
            
            // ✅ 根据用户身份设置对应的删除标记
            boolean isSender = message.getSenderId().equals(userId);
            Boolean newDeletedBySender = null;
            Boolean newDeletedByReceiver = null;
            
            if (isSender) {
                // 发送方删除：设置 deletedBySender = true
                newDeletedBySender = true;
                message.setDeletedBySender(true);
            } else {
                // 接收方删除：设置 deletedByReceiver = true
                newDeletedByReceiver = true;
                message.setDeletedByReceiver(true);
            }
            
            // ✅ 保存消息的更改
            messageRepository.save(message);
            
            // ✅ 重新从数据库加载conversation，确保获取最新的最后消息字段
            conversation = conversationRepository.findById(message.getConversationId())
                    .orElseThrow(() -> new BusinessException("对话不存在"));
            
            // ✅ 如果可见性发生变化，需要更新相关用户的最后消息字段
            // 参考 AdminService.updateMessageFull 的逻辑
            if (newDeletedBySender != null && !newDeletedBySender.equals(oldDeletedBySender)) {
                // 发送方可见性发生了变化（从可见变为不可见）
                boolean wasVisible = !Boolean.TRUE.equals(oldDeletedBySender);
                boolean isNowVisible = !Boolean.TRUE.equals(newDeletedBySender);
                
                if (wasVisible && !isNowVisible) {
                    // 从可见变为不可见（标记删除）
                    // 检查是否是发送方的最后一条可见消息
                    String senderId = message.getSenderId();
                    String senderLastContent = conversation.getLastMessageContentForUser(senderId);
                    LocalDateTime senderLastTime = conversation.getLastMessageTimeForUser(senderId);
                    // ✅ 使用更宽松的比较：内容相同，时间相差不超过1秒（处理精度问题）
                    boolean isSenderLastMessage = senderLastContent != null && senderLastTime != null &&
                        message.getContent().equals(senderLastContent) && 
                        (message.getCreatedAt().equals(senderLastTime) || 
                         Math.abs(java.time.Duration.between(message.getCreatedAt(), senderLastTime).getSeconds()) <= 1);
                    
                    if (isSenderLastMessage) {
                        // 查询发送方可见的最后一条消息（因为当前消息已被标记删除，查询时会自动过滤）
                        try {
                            Pageable pageable = PageRequest.of(0, 1);
                            List<Message> lastMessages = messageRepository.findLastMessageForUser(
                                    message.getConversationId(), senderId, pageable);
                            
                            if (!lastMessages.isEmpty()) {
                                // 有可见消息，使用作为新的最后消息
                                Message newLastMessage = lastMessages.get(0);
                                conversation.setLastMessageForUser(senderId, 
                                        newLastMessage.getContent(), 
                                        newLastMessage.getCreatedAt());
                            } else {
                                // 没有其他可见消息了，设置为空
                                conversation.setLastMessageForUser(senderId, null, null);
                            }
                        } catch (Exception e) {
                            log.warn("删除消息时更新发送方最后消息失败: conversationId={}, messageId={}, error={}", 
                                    message.getConversationId(), messageId, e.getMessage());
                        }
                    }
                }
            }
            
            if (newDeletedByReceiver != null && !newDeletedByReceiver.equals(oldDeletedByReceiver)) {
                // 接收方可见性发生了变化（从可见变为不可见）
                boolean wasVisible = !Boolean.TRUE.equals(oldDeletedByReceiver);
                boolean isNowVisible = !Boolean.TRUE.equals(newDeletedByReceiver);
                
                if (wasVisible && !isNowVisible) {
                    // 从可见变为不可见（标记删除）
                    // 检查是否是接收方的最后一条可见消息
                    String receiverId = message.getReceiverId();
                    String receiverLastContent = conversation.getLastMessageContentForUser(receiverId);
                    LocalDateTime receiverLastTime = conversation.getLastMessageTimeForUser(receiverId);
                    // ✅ 使用更宽松的比较：内容相同，时间相差不超过1秒（处理精度问题）
                    boolean isReceiverLastMessage = receiverLastContent != null && receiverLastTime != null &&
                        message.getContent().equals(receiverLastContent) && 
                        (message.getCreatedAt().equals(receiverLastTime) || 
                         Math.abs(java.time.Duration.between(message.getCreatedAt(), receiverLastTime).getSeconds()) <= 1);
                    
                    if (isReceiverLastMessage) {
                        // 查询接收方可见的最后一条消息（因为当前消息已被标记删除，查询时会自动过滤）
                        try {
                            Pageable pageable = PageRequest.of(0, 1);
                            List<Message> lastMessages = messageRepository.findLastMessageForUser(
                                    message.getConversationId(), receiverId, pageable);
                            
                            if (!lastMessages.isEmpty()) {
                                // 有可见消息，使用作为新的最后消息
                                Message newLastMessage = lastMessages.get(0);
                                conversation.setLastMessageForUser(receiverId, 
                                        newLastMessage.getContent(), 
                                        newLastMessage.getCreatedAt());
                            } else {
                                // 没有其他可见消息了，设置为空
                                conversation.setLastMessageForUser(receiverId, null, null);
                            }
                        } catch (Exception e) {
                            log.warn("删除消息时更新接收方最后消息失败: conversationId={}, messageId={}, error={}", 
                                    message.getConversationId(), messageId, e.getMessage());
                        }
                    }
                }
            }
            
            // ✅ 保存对话的更新（如果有任何字段被更新）
            conversationRepository.save(conversation);
            
            return Result.ok("删除消息成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("删除消息失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result searchMessages(String userId, String conversationId, 
                                 String keyword, int page, int size) {
        try {
            // 验证对话权限
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                throw new BusinessException("对话不存在");
            }
            
            Conversation conversation = convOpt.get();
            // 验证权限（使用 involvesUser 方法，基于 user_id_1 和 user_id_2）
            if (!conversation.involvesUser(userId)) {
                throw new BusinessException("无权访问此对话");
            }
            
            // 搜索消息
            Pageable pageable = PageRequest.of(page, size);
            Page<Message> messagesPage = messageRepository.searchMessages(conversationId, keyword, pageable);
            
            // ✅ 优化：批量查询两个用户的 UserProfile（使用Feign Client）
            Set<String> userIds = new HashSet<>();
            userIds.add(conversation.getUserId1());
            userIds.add(conversation.getUserId2());
            
            Map<String, UserProfileInternalDTO> profileMapTemp = new HashMap<>();
            if (!userIds.isEmpty()) {
                Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
                if (profileResult.getSuccess() && profileResult.getData() != null) {
                    try {
                        List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                            profileResult.getData(),
                            new TypeReference<List<UserProfileInternalDTO>>() {}
                        );
                        profileMapTemp = profiles.stream()
                                .collect(Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                    } catch (Exception e) {
                        log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                    }
                }
            }
            final Map<String, UserProfileInternalDTO> finalProfileMap = profileMapTemp;
            
            // ✅ 使用批量查询的 Map，不再单独查询
            Page<MessageDTO> dtoPage = messagesPage.map(message -> convertMessageToDTOWithMap(message, userId, finalProfileMap));
            
            return Result.ok("搜索消息成功", dtoPage);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("搜索消息失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result getConversationWithUser(String userId, String otherUserId) {
        try {
            Optional<Conversation> convOpt = conversationRepository.findByBuyerIdAndSellerId(userId, otherUserId);
            
            if (convOpt.isPresent()) {
                ConversationDTO dto = convertConversationToDTO(convOpt.get(), userId);
                return Result.ok("获取对话成功", dto);
            } else {
                throw new BusinessException("对话不存在");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("获取对话失败，请稍后重试", e);
        }
    }
    
    @Override
    public Result blockUser(String userId, String blockedUserId, String reason) {
        // 黑名单功能暂不实现
        return Result.ok("功能开发中");
    }
    
    @Override
    public Result unblockUser(String userId, String blockedUserId) {
        // 黑名单功能暂不实现
        return Result.ok("功能开发中");
    }
    
    @Override
    public Result isBlocked(String userId, String otherUserId) {
        // 黑名单功能暂不实现
        return Result.ok("检查成功", false);
    }
    
    // 辅助方法：转换Conversation为DTO（使用批量查询的 Map，优化 N+1 查询）
    private ConversationDTO convertConversationToDTOWithMap(Conversation conversation, String currentUserId, 
                                                              Map<String, UserProfileInternalDTO> profileMap, 
                                                              Map<String, User> userMap) {
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation不能为null");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("currentUserId不能为null");
        }
        if (profileMap == null) {
            profileMap = new HashMap<>();
        }
        if (userMap == null) {
            userMap = new HashMap<>();
        }
        
        ConversationDTO dto = new ConversationDTO();
        dto.setConversationId(conversation.getConversationId());
        
        // ✅ 用户端：使用用户级别的最后消息字段（过滤用户删除的）
        // 使用辅助方法获取对应用户的最后消息
        dto.setLastMessageContent(conversation.getLastMessageContentForUser(currentUserId));
        dto.setLastMessageTime(conversation.getLastMessageTimeForUser(currentUserId));
        
        dto.setStatus(conversation.getStatus());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        
        // 设置未读数（如果为null则设为0）
        Integer unreadCount = conversation.getUnreadCountForUser(currentUserId);
        dto.setUnreadCount(unreadCount != null ? unreadCount : 0);
        
        // 获取对方用户ID
        String otherUserId = conversation.getOtherUserId(currentUserId);
        dto.setOtherUserId(otherUserId);
        
        // ✅ 从 Map 中获取对方用户信息，不再查询数据库
        if (otherUserId != null) {
            UserProfileInternalDTO profile = profileMap.get(otherUserId);
            if (profile != null) {
                dto.setOtherUserNickname(profile.getNickname());
                dto.setOtherUserAvatar(profile.getAvatar());
            }
            
            // ✅ 从 Map 中检查用户是否已注销
            User user = userMap.get(otherUserId);
            dto.setOtherUserIsDeleted(
                user == null || "DELETED".equals(user.getAccountStatus())
            );
        }
        
        // ✅ 从 Map 中获取用户1的信息（用于兼容显示）
        if (conversation.getUserId1() != null) {
            UserProfileInternalDTO user1Profile = profileMap.get(conversation.getUserId1());
            if (user1Profile != null) {
                // 如果当前用户是 user1，则 user1 的信息对应 "buyer"，否则对应 "seller"
                // 为了向后兼容，这里统一设置为 buyerNickname/buyerAvatar
                dto.setBuyerNickname(user1Profile.getNickname());
                dto.setBuyerAvatar(user1Profile.getAvatar());
            }
        }
        
        // ✅ 从 Map 中获取用户2的信息（用于兼容显示）
        if (conversation.getUserId2() != null) {
            UserProfileInternalDTO user2Profile = profileMap.get(conversation.getUserId2());
            if (user2Profile != null) {
                // 为了向后兼容，这里统一设置为 sellerNickname/sellerAvatar
                dto.setSellerNickname(user2Profile.getNickname());
                dto.setSellerAvatar(user2Profile.getAvatar());
            }
        }
        
        return dto;
    }
    
    // 辅助方法：转换Conversation为DTO（保留原方法用于单条对话场景，如 getOrCreateConversation）
    private ConversationDTO convertConversationToDTO(Conversation conversation, String currentUserId) {
        ConversationDTO dto = new ConversationDTO();
        dto.setConversationId(conversation.getConversationId());
        
        // ✅ 用户端：使用用户级别的最后消息字段（过滤用户删除的）
        // 使用辅助方法获取对应用户的最后消息
        dto.setLastMessageContent(conversation.getLastMessageContentForUser(currentUserId));
        dto.setLastMessageTime(conversation.getLastMessageTimeForUser(currentUserId));
        
        dto.setStatus(conversation.getStatus());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        
        // 设置未读数
        dto.setUnreadCount(conversation.getUnreadCountForUser(currentUserId));
        
        // 获取对方用户ID
        String otherUserId = conversation.getOtherUserId(currentUserId);
        dto.setOtherUserId(otherUserId);
        
        // ✅ 查询对方用户信息（单条对话场景使用，使用Feign Client）
        if (otherUserId != null) {
            Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(java.util.Collections.singletonList(otherUserId)));
            if (profileResult.getSuccess() && profileResult.getData() != null) {
                try {
                    List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                        profileResult.getData(),
                        new TypeReference<List<UserProfileInternalDTO>>() {}
                    );
                    if (!profiles.isEmpty()) {
                        UserProfileInternalDTO profile = profiles.get(0);
                        dto.setOtherUserNickname(profile.getNickname());
                        dto.setOtherUserAvatar(profile.getAvatar());
                    }
                } catch (Exception e) {
                    log.error("转换UserProfileInternalDTO失败: {}", e.getMessage(), e);
                }
            }
            
            // ✅ 检查用户是否已注销（使用Feign Client）
            Result userResult = authClient.getUserById(otherUserId);
            boolean isDeleted = false;
            if (userResult.getSuccess() && userResult.getData() != null) {
                try {
                    // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
                    UserInternalDTO userDTO = objectMapper.convertValue(
                        userResult.getData(),
                        new TypeReference<UserInternalDTO>() {}
                    );
                    isDeleted = "DELETED".equals(userDTO.getAccountStatus());
                } catch (Exception e) {
                    log.error("转换User失败: otherUserId={}, error={}", otherUserId, e.getMessage(), e);
                    // 如果转换失败，默认认为用户未注销
                    isDeleted = false;
                }
            } else {
                // 如果查询失败，默认认为用户已注销
                isDeleted = true;
            }
            dto.setOtherUserIsDeleted(isDeleted);
        }
        
        // ✅ 查询用户1和用户2的信息（用于兼容显示，使用Feign Client批量查询）
        List<String> userIds = new ArrayList<>();
        userIds.add(conversation.getUserId1());
        userIds.add(conversation.getUserId2());
        Result profilesResult = authClient.getUserProfilesByIds(userIds);
        if (profilesResult.getSuccess() && profilesResult.getData() != null) {
            try {
                List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                    profilesResult.getData(),
                    new TypeReference<List<UserProfileInternalDTO>>() {}
                );
                Map<String, UserProfileInternalDTO> profileMap = profiles.stream()
                    .collect(java.util.stream.Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                
                // 查询用户1的信息
                UserProfileInternalDTO user1Profile = profileMap.get(conversation.getUserId1());
                if (user1Profile != null) {
                    // 如果当前用户是 user1，则 user1 的信息对应 "buyer"，否则对应 "seller"
                    // 为了向后兼容，这里统一设置为 buyerNickname/buyerAvatar
                    dto.setBuyerNickname(user1Profile.getNickname());
                    dto.setBuyerAvatar(user1Profile.getAvatar());
                }
                
                // 查询用户2的信息
                UserProfileInternalDTO user2Profile = profileMap.get(conversation.getUserId2());
                if (user2Profile != null) {
                    // 为了向后兼容，这里统一设置为 sellerNickname/sellerAvatar
                    dto.setSellerNickname(user2Profile.getNickname());
                    dto.setSellerAvatar(user2Profile.getAvatar());
                }
            } catch (Exception e) {
                log.error("转换UserProfileInternalDTO失败: {}", e.getMessage(), e);
            }
        }
        
        return dto;
    }
    
    // 辅助方法：转换Message为DTO（使用批量查询的 Map）
    private MessageDTO convertMessageToDTOWithMap(Message message, String currentUserId, Map<String, UserProfileInternalDTO> profileMap) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setMessageType(message.getMessageType());
        dto.setContent(message.getContent());
        dto.setImageUrl(message.getImageUrl());
        // 实时商品和订单ID（用于商品/订单卡片）
        dto.setCommodityId(message.getCommodityId());
        dto.setOrderId(message.getOrderId());
        dto.setIsRead(message.getIsRead());
        dto.setReadTime(message.getReadTime());
        dto.setCreatedAt(message.getCreatedAt());
        
        // 设置是否是当前用户发送的消息
        dto.setIsMine(message.getSenderId().equals(currentUserId));
        
        // ✅ 从 Map 中获取发送者信息，不再查询数据库
        UserProfileInternalDTO senderProfile = profileMap.get(message.getSenderId());
        if (senderProfile != null) {
            dto.setSenderNickname(senderProfile.getNickname());
            dto.setSenderAvatar(senderProfile.getAvatar());
        }
        
        return dto;
    }
    
    // 辅助方法：转换Message为DTO（保留原方法用于单条消息，如 sendMessage）
    private MessageDTO convertMessageToDTO(Message message, String currentUserId) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setMessageType(message.getMessageType());
        dto.setContent(message.getContent());
        dto.setImageUrl(message.getImageUrl());
        // 实时商品和订单ID（用于商品/订单卡片）
        dto.setCommodityId(message.getCommodityId());
        dto.setOrderId(message.getOrderId());
        dto.setIsRead(message.getIsRead());
        dto.setReadTime(message.getReadTime());
        dto.setCreatedAt(message.getCreatedAt());
        
        // 设置是否是当前用户发送的消息
        dto.setIsMine(message.getSenderId().equals(currentUserId));
        
        // ✅ 查询发送者信息（单条消息场景使用，使用Feign Client批量查询）
        Result senderProfileResult = authClient.getUserProfilesByIds(new ArrayList<>(java.util.Collections.singletonList(message.getSenderId())));
        if (senderProfileResult.getSuccess() && senderProfileResult.getData() != null) {
            try {
                List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                    senderProfileResult.getData(),
                    new TypeReference<List<UserProfileInternalDTO>>() {}
                );
                if (!profiles.isEmpty()) {
                    UserProfileInternalDTO senderProfile = profiles.get(0);
                    dto.setSenderNickname(senderProfile.getNickname());
                    dto.setSenderAvatar(senderProfile.getAvatar());
                }
            } catch (Exception e) {
                log.error("转换UserProfileInternalDTO失败: {}", e.getMessage(), e);
            }
        }
        
        return dto;
    }
}