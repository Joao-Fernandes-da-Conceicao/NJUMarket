package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.ConversationDTO;
import com.njumarket.njumarket.dto.MessageDTO;
import com.njumarket.njumarket.dto.SendMessageRequest;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.entity.*;
import com.njumarket.njumarket.repository.*;
import com.njumarket.njumarket.service.ContactService;

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
    
    private final UserRepository userRepository;
    
    private final UserProfileRepository userProfileRepository;
    
    private final OrderRepository orderRepository;
    
    private final SimpMessagingTemplate messagingTemplate;
    
    private final com.njumarket.njumarket.service.WebSocketRetryService webSocketRetryService;
    
    @Override
    public Result sendMessage(String userId, SendMessageRequest request) {
        try {
            // 验证接收者是否存在
            Optional<User> receiverOpt = userRepository.findById(request.getReceiverId());
            if (!receiverOpt.isPresent()) {
                return Result.fail("接收者不存在");
            }
            
            // 获取或创建对话
            Conversation conversation;
            if (request.getConversationId() != null) {
                Optional<Conversation> convOpt = conversationRepository.findById(request.getConversationId());
                if (!convOpt.isPresent()) {
                    return Result.fail("对话不存在");
                }
                conversation = convOpt.get();
                
                // 验证用户是否属于这个对话
                if (!conversation.involvesUser(userId)) {
                    return Result.fail("无权访问此对话");
                }
            } else {
                // 创建新对话（基于用户对，确保唯一性）
                Optional<Conversation> existingConv = conversationRepository.findByUserPairActive(userId, request.getReceiverId());
                if (existingConv.isPresent()) {
                    conversation = existingConv.get();
                } else {
                    conversation = new Conversation();
                    conversation.setUserPair(userId, request.getReceiverId());
                    conversation.setStatus("ACTIVE");
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
                // TODO: 商品卡片功能（需要卖家商品查询页面）
                // 暂时设置commodityId字段，但返回错误提示
                message.setCommodityId(request.getCommodityId());
                message.setMessageType("COMMODITY_CARD");
                return Result.fail("商品卡片功能暂未实现");
            }
            
            if ("ORDER_CARD".equals(request.getMessageType()) && request.getOrderId() != null) {
                // 验证订单是否属于对话双方
                Optional<Order> orderOpt = orderRepository.findById(request.getOrderId());
                if (orderOpt.isEmpty()) {
                    return Result.fail("订单不存在");
                }
                Order order = orderOpt.get();
                String otherUserId = conversation.getOtherUserId(userId);
                if (otherUserId == null) {
                    return Result.fail("无法确定对话对方用户");
                }
                
                // 验证订单的买卖双方必须匹配对话的双方用户
                boolean buyerMatches = order.getBuyerId().equals(userId) || order.getBuyerId().equals(otherUserId);
                boolean sellerMatches = order.getSellerId().equals(userId) || order.getSellerId().equals(otherUserId);
                if (!buyerMatches || !sellerMatches) {
                    return Result.fail("无权发送此订单卡片：订单不属于当前对话双方");
                }
                
                // 设置订单卡片相关字段
                message.setOrderId(request.getOrderId());
                message.setMessageType("ORDER_CARD");
                if (message.getContent() == null || message.getContent().isEmpty()) {
                    message.setContent("订单：" + order.getOrderId());
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
            
            // 更新对话最后消息
            conversation.setLastMessageContent(request.getContent());
            conversation.setLastMessageTime(LocalDateTime.now());
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
            log.debug("未读数更新推送尝试（带重试）: receiverId={}, totalUnreadCount={}, conversationUnreadCount={}", 
                    receiverId, totalUnreadCount, conversationUnreadCount);
            
            return Result.ok("消息发送成功", messageDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("发送消息失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result getConversations(String userId, int page, int size) {
        try {
            // ✅ 优化：使用数据库分页，而不是内存分页
            // 创建分页对象，按最后消息时间降序排序
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageTime"));
            
            // 查询用户的所有活跃对话（数据库分页）
            Page<Conversation> conversationsPage = conversationRepository.findByUserIdAndStatus(userId, "ACTIVE", pageable);
            List<Conversation> pagedConversations = conversationsPage.getContent();
            
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
            
            // ✅ 优化：批量查询边界处理 - 只在有用户ID时才查询
            final Map<String, UserProfile> profileMap;
            if (!userIds.isEmpty()) {
            List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
                profileMap = profiles.stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
            } else {
                profileMap = new HashMap<>();
            }
            
            // ✅ 优化：批量查询 User（用于检查用户是否已注销）
            final Map<String, User> userMap;
            if (!userIdsForUserCheck.isEmpty()) {
            List<User> users = userRepository.findAllById(userIdsForUserCheck);
                userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));
            } else {
                userMap = new HashMap<>();
            }
            
            // 转换为 DTO（使用批量查询的 Map）
            List<ConversationDTO> dtoList = pagedConversations.stream()
                .map(conversation -> convertConversationToDTOWithMap(conversation, userId, profileMap, userMap))
                .collect(Collectors.toList());
            
            // 构造分页结果（使用数据库分页的总数）
            Page<ConversationDTO> dtoPage = new PageImpl<>(dtoList, pageable, conversationsPage.getTotalElements());
            
            return Result.ok("获取对话列表成功", dtoPage);
        } catch (Exception e) {
            log.error("获取对话列表失败: userId={}, error={}", userId, e.getMessage());
            return Result.fail("获取对话列表失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result getConversationDetail(String userId, String conversationId, int page, int size) {
        try {
            // 查询对话
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                return Result.fail("对话不存在");
            }
            
            Conversation conversation = convOpt.get();
            
            // 验证权限（使用 involvesUser 方法，基于 user_id_1 和 user_id_2）
            if (!conversation.involvesUser(userId)) {
                return Result.fail("无权访问此对话");
            }
            
            // 查询消息列表
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> messagesPage = messageRepository.findByConversationId(conversationId, pageable);
            
            // ✅ 优化：批量查询两个用户的 UserProfile（只需要2次查询）
            // 在一个对话中，消息的发送者只有两种可能：当前用户或对方用户
            Set<String> userIds = new HashSet<>();
            userIds.add(conversation.getUserId1());
            userIds.add(conversation.getUserId2());
            
            List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
            Map<String, UserProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
            
            // 转换对话和消息为DTO，并过滤当前用户已删除的消息
            ConversationDTO dto = convertConversationToDTO(conversation, userId);
            
            List<MessageDTO> messageDTOs = new ArrayList<>();
            for (Message message : messagesPage.getContent()) {
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
                    messageDTOs.add(convertMessageToDTOWithMap(message, userId, profileMap));
                }
            }
            
            dto.setMessages(messageDTOs);
            dto.setTotalMessages(messageDTOs.size());
            
            return Result.ok("获取对话详情成功", dto);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("获取对话详情失败：" + e.getMessage());
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
        } catch (Exception e) {
            log.error("获取或创建对话失败: userId={}, otherUserId={}, error={}", userId, otherUserId, e.getMessage());
            return Result.fail("获取或创建对话失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result markConversationAsRead(String userId, String conversationId) {
        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                return Result.fail("对话不存在");
            }
            
            Conversation conversation = convOpt.get();
            
            // 验证权限（使用 involvesUser 方法，基于 user_id_1 和 user_id_2）
            if (!conversation.involvesUser(userId)) {
                return Result.fail("无权访问此对话");
            }
            
            // 标记对话为已读
            conversation.markAsReadForUser(userId);
            conversationRepository.save(conversation);
            
            // 标记所有消息为已读
            messageRepository.markMessagesAsRead(conversationId, userId, LocalDateTime.now());
            
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
                log.debug("未读数更新推送尝试（带重试）: userId={}, totalUnreadCount={}, conversationUnreadCount={}", 
                        userId, totalUnreadCount, conversationUnreadCount);
            } catch (Exception e) {
                log.error("推送未读数更新失败: userId={}, error={}", userId, e.getMessage(), e);
                // WebSocket 推送失败不影响标记已读的成功返回
            }
            
            return Result.ok("标记已读成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("标记已读失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result getUnreadCount(String userId) {
        try {
            Integer count = conversationRepository.getTotalUnreadCount(userId);
            return Result.ok("获取未读数成功", count != null ? count : 0);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("获取未读数失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result deleteConversation(String userId, String conversationId) {
        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                return Result.fail("对话不存在");
            }
            
            Conversation conversation = convOpt.get();
            if (!conversation.involvesUser(userId)) {
                return Result.fail("无权删除此对话");
            }
            
            conversationRepository.softDelete(conversationId);
            return Result.ok("删除对话成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("删除对话失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result deleteMessage(String userId, String messageId) {
        try {
            Optional<Message> msgOpt = messageRepository.findById(messageId);
            if (!msgOpt.isPresent()) {
                return Result.fail("消息不存在");
            }
            
            Message message = msgOpt.get();
            
            // 验证权限：只能删除对话中自己的消息
            if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
                return Result.fail("无权删除此消息");
            }
            
            // 双向删除：根据用户身份设置对应的删除标记
            if (message.getSenderId().equals(userId)) {
                // 发送方删除
                messageRepository.markDeletedBySender(messageId);
            } else if (message.getReceiverId().equals(userId)) {
                // 接收方删除
                messageRepository.markDeletedByReceiver(messageId);
            }
            
            return Result.ok("删除消息成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("删除消息失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result searchMessages(String userId, String conversationId, 
                                 String keyword, int page, int size) {
        try {
            // 验证对话权限
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                return Result.fail("对话不存在");
            }
            
            Conversation conversation = convOpt.get();
            // 验证权限（使用 involvesUser 方法，基于 user_id_1 和 user_id_2）
            if (!conversation.involvesUser(userId)) {
                return Result.fail("无权访问此对话");
            }
            
            // 搜索消息
            Pageable pageable = PageRequest.of(page, size);
            Page<Message> messagesPage = messageRepository.searchMessages(conversationId, keyword, pageable);
            
            // ✅ 优化：批量查询两个用户的 UserProfile（只需要2次查询）
            Set<String> userIds = new HashSet<>();
            userIds.add(conversation.getUserId1());
            userIds.add(conversation.getUserId2());
            
            List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
            Map<String, UserProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
            
            // ✅ 使用批量查询的 Map，不再单独查询
            Page<MessageDTO> dtoPage = messagesPage.map(message -> convertMessageToDTOWithMap(message, userId, profileMap));
            
            return Result.ok("搜索消息成功", dtoPage);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("搜索消息失败：" + e.getMessage());
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
                return Result.fail("对话不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("获取对话失败：" + e.getMessage());
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
                                                              Map<String, UserProfile> profileMap, 
                                                              Map<String, User> userMap) {
        ConversationDTO dto = new ConversationDTO();
        dto.setConversationId(conversation.getConversationId());
        
        dto.setLastMessageContent(conversation.getLastMessageContent());
        dto.setLastMessageTime(conversation.getLastMessageTime());
        dto.setStatus(conversation.getStatus());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        
        // 设置未读数
        dto.setUnreadCount(conversation.getUnreadCountForUser(currentUserId));
        
        // 获取对方用户ID
        String otherUserId = conversation.getOtherUserId(currentUserId);
        dto.setOtherUserId(otherUserId);
        
        // ✅ 从 Map 中获取对方用户信息，不再查询数据库
        if (otherUserId != null) {
            UserProfile profile = profileMap.get(otherUserId);
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
        UserProfile user1Profile = profileMap.get(conversation.getUserId1());
        if (user1Profile != null) {
            // 如果当前用户是 user1，则 user1 的信息对应 "buyer"，否则对应 "seller"
            // 为了向后兼容，这里统一设置为 buyerNickname/buyerAvatar
            dto.setBuyerNickname(user1Profile.getNickname());
            dto.setBuyerAvatar(user1Profile.getAvatar());
        }
        
        // ✅ 从 Map 中获取用户2的信息（用于兼容显示）
        UserProfile user2Profile = profileMap.get(conversation.getUserId2());
        if (user2Profile != null) {
            // 为了向后兼容，这里统一设置为 sellerNickname/sellerAvatar
            dto.setSellerNickname(user2Profile.getNickname());
            dto.setSellerAvatar(user2Profile.getAvatar());
        }
        
        return dto;
    }
    
    // 辅助方法：转换Conversation为DTO（保留原方法用于单条对话场景，如 getOrCreateConversation）
    private ConversationDTO convertConversationToDTO(Conversation conversation, String currentUserId) {
        ConversationDTO dto = new ConversationDTO();
        dto.setConversationId(conversation.getConversationId());
        
        dto.setLastMessageContent(conversation.getLastMessageContent());
        dto.setLastMessageTime(conversation.getLastMessageTime());
        dto.setStatus(conversation.getStatus());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        
        // 设置未读数
        dto.setUnreadCount(conversation.getUnreadCountForUser(currentUserId));
        
        // 获取对方用户ID
        String otherUserId = conversation.getOtherUserId(currentUserId);
        dto.setOtherUserId(otherUserId);
        
        // 查询对方用户信息（单条对话场景使用）
        if (otherUserId != null) {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(otherUserId);
            if (profileOpt.isPresent()) {
                UserProfile profile = profileOpt.get();
                dto.setOtherUserNickname(profile.getNickname());
                dto.setOtherUserAvatar(profile.getAvatar());
            }
            
            // 检查用户是否已注销
            Optional<User> userOpt = userRepository.findById(otherUserId);
            dto.setOtherUserIsDeleted(
                !userOpt.isPresent() || "DELETED".equals(userOpt.get().getAccountStatus())
            );
        }
        
        // 查询用户1的信息（用于兼容显示）
        Optional<UserProfile> user1ProfileOpt = userProfileRepository.findByUserId(conversation.getUserId1());
        if (user1ProfileOpt.isPresent()) {
            UserProfile user1Profile = user1ProfileOpt.get();
            // 如果当前用户是 user1，则 user1 的信息对应 "buyer"，否则对应 "seller"
            // 为了向后兼容，这里统一设置为 buyerNickname/buyerAvatar
            dto.setBuyerNickname(user1Profile.getNickname());
            dto.setBuyerAvatar(user1Profile.getAvatar());
        }
        
        // 查询用户2的信息（用于兼容显示）
        Optional<UserProfile> user2ProfileOpt = userProfileRepository.findByUserId(conversation.getUserId2());
        if (user2ProfileOpt.isPresent()) {
            UserProfile user2Profile = user2ProfileOpt.get();
            // 为了向后兼容，这里统一设置为 sellerNickname/sellerAvatar
            dto.setSellerNickname(user2Profile.getNickname());
            dto.setSellerAvatar(user2Profile.getAvatar());
        }
        
        return dto;
    }
    
    // 辅助方法：转换Message为DTO（使用批量查询的 Map）
    private MessageDTO convertMessageToDTOWithMap(Message message, String currentUserId, Map<String, UserProfile> profileMap) {
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
        UserProfile senderProfile = profileMap.get(message.getSenderId());
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
        
        // 查询发送者信息（单条消息场景使用）
        Optional<UserProfile> senderProfileOpt = userProfileRepository.findByUserId(message.getSenderId());
        if (senderProfileOpt.isPresent()) {
            UserProfile senderProfile = senderProfileOpt.get();
            dto.setSenderNickname(senderProfile.getNickname());
            dto.setSenderAvatar(senderProfile.getAvatar());
        }
        
        return dto;
    }
}