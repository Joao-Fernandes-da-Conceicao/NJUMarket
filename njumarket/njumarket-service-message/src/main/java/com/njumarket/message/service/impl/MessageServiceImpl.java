package com.njumarket.message.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.message.client.CommodityClient;
import com.njumarket.message.client.OrderClient;
import com.njumarket.message.dto.ConversationDTO;
import com.njumarket.message.dto.MessageDTO;
import com.njumarket.message.dto.SendMessageRequest;
import com.njumarket.message.entity.Conversation;
import com.njumarket.message.entity.CommoditySnapshot;
import com.njumarket.message.entity.Message;
import com.njumarket.message.entity.User;
import com.njumarket.message.repository.CommoditySnapshotRepository;
import com.njumarket.message.repository.ConversationRepository;
import com.njumarket.message.repository.MessageRepository;
import com.njumarket.message.service.MessageService;
import com.njumarket.message.service.UserCacheService;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.njumarket.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CommoditySnapshotRepository commoditySnapshotRepository;
    private final CommodityClient commodityClient;
    private final OrderClient orderClient;
    private final ObjectMapper objectMapper;
    private final UserCacheService userCacheService;
    private final com.njumarket.message.mq.MessagePushEventProducer messagePushEventProducer;

    // ==================== 发送消息 ====================

    @Override
    public Result sendMessage(String userId, SendMessageRequest request) {
        try {
            Object userObj = com.njumarket.njumarket.utils.SecurityUtils.requireCurrentUser();
            User currentUser = (User) userObj;

            if (!currentUser.getUserId().equals(userId)) {
                throw new BusinessException("无权操作：用户ID不匹配");
            }

            if (!"ACTIVE".equals(currentUser.getAccountStatus())) {
                throw new BusinessException("账户已被禁用，无法发送消息");
            }

            UserInternalDTO receiverDTO = userCacheService.getUserById(request.getReceiverId());
            if (receiverDTO == null) {
                throw new BusinessException("接收者不存在");
            }

            User receiver = new User();
            receiver.setUserId(receiverDTO.getUserId());
            receiver.setUsername(receiverDTO.getUsername());
            receiver.setPrimaryPhone(receiverDTO.getPrimaryPhone());
            receiver.setAccountStatus(receiverDTO.getAccountStatus());
            receiver.setRegisterTime(receiverDTO.getRegisterTime());

            if (!"ACTIVE".equals(receiver.getAccountStatus())) {
                throw new BusinessException("接收者账户已被禁用");
            }

            if (userId.equals(request.getReceiverId())) {
                throw new BusinessException("不能向自己发送消息");
            }

            Conversation conversation;
            if (request.getConversationId() != null) {
                Optional<Conversation> convOpt = conversationRepository.findById(request.getConversationId());
                if (!convOpt.isPresent()) {
                    throw new BusinessException("对话不存在");
                }
                conversation = convOpt.get();

                if (!conversation.involvesUser(userId)) {
                    throw new BusinessException("无权访问此对话");
                }

                boolean visibilityRestored = false;
                if (!conversation.getVisibilityForUser(request.getReceiverId())) {
                    conversation.restoreVisibilityForUser(request.getReceiverId());
                    visibilityRestored = true;
                    log.debug("恢复接收方会话可见性: conversationId={}, receiverId={}",
                            conversation.getConversationId(), request.getReceiverId());
                }

                if (visibilityRestored) {
                    pushConversationRestoredEvent(conversation, request.getReceiverId());
                }
            } else {
                Optional<Conversation> existingConv = conversationRepository.findByUserPairActive(userId, request.getReceiverId());
                if (existingConv.isPresent()) {
                    conversation = existingConv.get();
                    boolean visibilityRestored = false;
                    if (!conversation.getVisibilityForUser(request.getReceiverId())) {
                        conversation.restoreVisibilityForUser(request.getReceiverId());
                        visibilityRestored = true;
                    }

                    if (visibilityRestored) {
                        pushConversationRestoredEvent(conversation, request.getReceiverId());
                    }
                } else {
                    conversation = new Conversation();
                    conversation.setUserPair(userId, request.getReceiverId());
                    conversation.setStatus("ACTIVE");
                    conversation.setUser1Visibility(true);
                    conversation.setUser2Visibility(true);
                    conversationRepository.save(conversation);
                }
            }

            Message message = new Message();
            message.setConversationId(conversation.getConversationId());
            message.setSenderId(userId);
            message.setReceiverId(request.getReceiverId());
            message.setMessageType(request.getMessageType() != null ? request.getMessageType() : "TEXT");
            message.setContent(request.getContent());
            message.setImageUrl(request.getImageUrl());

            // 商品快照数据（仅 COMMODITY_CARD 时填充，用于保存快照）
            Map<String, Object> commodityDataForSnapshot = null;

            if ("COMMODITY_CARD".equals(request.getMessageType()) && request.getCommodityId() != null) {
                Result commodityResult = commodityClient.getCommodityById(request.getCommodityId());
                if (!commodityResult.getSuccess() || commodityResult.getData() == null) {
                    throw new BusinessException("商品不存在");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> commodityData = (Map<String, Object>) commodityResult.getData();
                commodityDataForSnapshot = commodityData;

                String sellerId = (String) commodityData.get("sellerId");
                String title = (String) commodityData.get("title");
                String commodityStatus = (String) commodityData.get("commodityStatus");

                String otherUserId = conversation.getOtherUserId(userId);
                if (otherUserId == null) {
                    throw new BusinessException("无法确定对话对方用户");
                }

                boolean sellerIsSender = sellerId != null && sellerId.equals(userId);
                boolean sellerIsReceiver = sellerId != null && sellerId.equals(otherUserId);

                if (!sellerIsSender && !sellerIsReceiver) {
                    throw new BusinessException("无权发送此商品卡片：商品不属于当前对话双方");
                }

                if ("OFF_SHELF".equals(commodityStatus) || "DRAFT".equals(commodityStatus)) {
                    throw new BusinessException("商品状态不允许发送：商品已下架或为草稿状态");
                }

                message.setCommodityId(request.getCommodityId());
                message.setMessageType("COMMODITY_CARD");
                if (message.getContent() == null || message.getContent().isEmpty()) {
                    message.setContent("商品：" + (title != null ? title : "未知商品"));
                }
            }

            if ("ORDER_CARD".equals(request.getMessageType()) && request.getOrderId() != null) {
                Result orderResult = orderClient.getOrderById(request.getOrderId());
                if (!orderResult.getSuccess() || orderResult.getData() == null) {
                    throw new BusinessException("订单不存在");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> orderData = (Map<String, Object>) orderResult.getData();
                String orderId = (String) orderData.get("orderId");
                String buyerId = (String) orderData.get("buyerId");
                String sellerId = (String) orderData.get("sellerId");
                String otherUserId = conversation.getOtherUserId(userId);
                if (otherUserId == null) {
                    throw new BusinessException("无法确定对话对方用户");
                }

                boolean buyerMatches = buyerId != null && (buyerId.equals(userId) || buyerId.equals(otherUserId));
                boolean sellerMatches = sellerId != null && (sellerId.equals(userId) || sellerId.equals(otherUserId));
                if (!buyerMatches || !sellerMatches) {
                    throw new BusinessException("无权发送此订单卡片：订单不属于当前对话双方");
                }

                message.setOrderId(request.getOrderId());
                message.setMessageType("ORDER_CARD");
                if (message.getContent() == null || message.getContent().isEmpty()) {
                    message.setContent("订单：" + (orderId != null ? orderId : request.getOrderId()));
                }
            }

            if (request.getCommodityId() != null && !"COMMODITY_CARD".equals(request.getMessageType())) {
                message.setCommodityId(request.getCommodityId());
            }
            if (request.getOrderId() != null && !"ORDER_CARD".equals(request.getMessageType())) {
                message.setOrderId(request.getOrderId());
            }

            message.setIsRead(false);
            messageRepository.save(message);

            // 保存商品快照（COMMODITY_CARD 类型消息）
            CommoditySnapshot savedSnapshot = null;
            if (commodityDataForSnapshot != null) {
                try {
                    CommoditySnapshot snapshot = new CommoditySnapshot();
                    snapshot.setMessageId(message.getMessageId());
                    snapshot.setCommodityId(message.getCommodityId());
                    snapshot.setTitle((String) commodityDataForSnapshot.get("title"));
                    Object priceObj = commodityDataForSnapshot.get("price");
                    if (priceObj instanceof Number) {
                        snapshot.setPrice(((Number) priceObj).doubleValue());
                    }
                    snapshot.setImageUrl(extractFirstImage((String) commodityDataForSnapshot.get("images")));
                    snapshot.setStatus((String) commodityDataForSnapshot.get("commodityStatus"));
                    savedSnapshot = commoditySnapshotRepository.save(snapshot);
                    log.debug("商品快照已保存: messageId={}, commodityId={}", message.getMessageId(), message.getCommodityId());
                } catch (Exception e) {
                    log.warn("保存商品快照失败（消息仍正常发送）: messageId={}, error={}", message.getMessageId(), e.getMessage());
                }
            }

            LocalDateTime now = LocalDateTime.now();
            String messageContent = request.getContent();

            conversation.setLastMessageContent(messageContent);
            conversation.setLastMessageTime(now);
            conversation.setLastMessageForUser(userId, messageContent, now);
            conversation.setLastMessageForUser(request.getReceiverId(), messageContent, now);
            conversation.incrementUnreadForUser(request.getReceiverId());
            conversationRepository.save(conversation);

            MessageDTO messageDTO = convertMessageToDTO(message, userId, savedSnapshot);

            String receiverId = request.getReceiverId();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageDataMap = objectMapper.convertValue(messageDTO, Map.class);
                if (!messageDataMap.containsKey("messageId") || messageDataMap.get("messageId") == null) {
                    messageDataMap.put("messageId", messageDTO.getMessageId());
                }
                if (!messageDataMap.containsKey("type") || messageDataMap.get("type") == null) {
                    messageDataMap.put("type", "MESSAGE_NEW");
                }
                messagePushEventProducer.sendMessagePushEvent(
                        receiverId,
                        messageDTO.getMessageId(),
                        messageDataMap,
                        "MESSAGE_NEW"
                );
                log.debug("消息推送任务已发送到MQ: receiverId={}, messageId={}", receiverId, messageDTO.getMessageId());
            } catch (Exception e) {
                log.warn("发送消息推送任务到MQ失败: receiverId={}, messageId={}, error={}",
                        receiverId, messageDTO.getMessageId(), e.getMessage());
            }

            Integer conversationUnreadCount = conversation.getUnreadCountForUser(receiverId);
            if (conversationUnreadCount == null) conversationUnreadCount = 0;

            Integer totalUnreadCount = conversationRepository.getTotalUnreadCount(receiverId);
            if (totalUnreadCount == null) totalUnreadCount = 0;

            Map<String, Object> unreadCountUpdate = new java.util.HashMap<>();
            unreadCountUpdate.put("type", "UNREAD_COUNT_UPDATE");
            unreadCountUpdate.put("unreadCount", totalUnreadCount);
            unreadCountUpdate.put("conversationId", conversation.getConversationId());
            unreadCountUpdate.put("conversationUnreadCount", conversationUnreadCount);
            unreadCountUpdate.put("timestamp", LocalDateTime.now().toString());

            try {
                messagePushEventProducer.sendMessagePushEvent(
                        receiverId,
                        "unread_count_" + receiverId + "_" + System.currentTimeMillis(),
                        unreadCountUpdate,
                        "UNREAD_COUNT_UPDATE"
                );
                log.debug("未读数更新推送任务已发送到MQ: receiverId={}, unreadCount={}", receiverId, totalUnreadCount);
            } catch (Exception e) {
                log.warn("发送未读数更新推送任务到MQ失败: receiverId={}, error={}", receiverId, e.getMessage());
            }

            return Result.ok("消息发送成功", messageDTO);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("发送消息失败，请稍后重试", e);
        }
    }

    // ==================== 删除消息 ====================

    @Override
    public Result deleteMessage(String userId, String messageId) {
        try {
            Optional<Message> msgOpt = messageRepository.findById(messageId);
            if (!msgOpt.isPresent()) {
                throw new BusinessException("消息不存在");
            }

            Message message = msgOpt.get();

            if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
                throw new BusinessException("无权删除此消息");
            }

            Optional<Conversation> convOpt = conversationRepository.findById(message.getConversationId());
            if (!convOpt.isPresent()) {
                throw new BusinessException("对话不存在");
            }
            Conversation conversation = convOpt.get();

            Boolean oldDeletedBySender = message.getDeletedBySender();
            Boolean oldDeletedByReceiver = message.getDeletedByReceiver();

            boolean isSender = message.getSenderId().equals(userId);
            Boolean newDeletedBySender = null;
            Boolean newDeletedByReceiver = null;

            if (isSender) {
                newDeletedBySender = true;
                message.setDeletedBySender(true);
            } else {
                newDeletedByReceiver = true;
                message.setDeletedByReceiver(true);
            }

            messageRepository.save(message);

            conversation = conversationRepository.findById(message.getConversationId())
                    .orElseThrow(() -> new BusinessException("对话不存在"));

            if (newDeletedBySender != null && !newDeletedBySender.equals(oldDeletedBySender)) {
                boolean wasVisible = !Boolean.TRUE.equals(oldDeletedBySender);
                boolean isNowVisible = !Boolean.TRUE.equals(newDeletedBySender);

                if (wasVisible && !isNowVisible) {
                    String senderId = message.getSenderId();
                    String senderLastContent = conversation.getLastMessageContentForUser(senderId);
                    LocalDateTime senderLastTime = conversation.getLastMessageTimeForUser(senderId);
                    boolean isSenderLastMessage = senderLastContent != null && senderLastTime != null &&
                        message.getContent().equals(senderLastContent) &&
                        (message.getCreatedAt().equals(senderLastTime) ||
                         Math.abs(java.time.Duration.between(message.getCreatedAt(), senderLastTime).getSeconds()) <= 1);

                    if (isSenderLastMessage) {
                        try {
                            Pageable pageable = PageRequest.of(0, 1);
                            List<com.njumarket.message.repository.projection.MessageContentProjection> lastMessages =
                                    messageRepository.findLastMessageContentForUser(
                                            message.getConversationId(), senderId, pageable);

                            if (!lastMessages.isEmpty()) {
                                com.njumarket.message.repository.projection.MessageContentProjection newLastMessage = lastMessages.get(0);
                                conversation.setLastMessageForUser(senderId,
                                        newLastMessage.getContent(),
                                        newLastMessage.getCreatedAt());
                            } else {
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
                boolean wasVisible = !Boolean.TRUE.equals(oldDeletedByReceiver);
                boolean isNowVisible = !Boolean.TRUE.equals(newDeletedByReceiver);

                if (wasVisible && !isNowVisible) {
                    String receiverId = message.getReceiverId();
                    String receiverLastContent = conversation.getLastMessageContentForUser(receiverId);
                    LocalDateTime receiverLastTime = conversation.getLastMessageTimeForUser(receiverId);
                    boolean isReceiverLastMessage = receiverLastContent != null && receiverLastTime != null &&
                        message.getContent().equals(receiverLastContent) &&
                        (message.getCreatedAt().equals(receiverLastTime) ||
                         Math.abs(java.time.Duration.between(message.getCreatedAt(), receiverLastTime).getSeconds()) <= 1);

                    if (isReceiverLastMessage) {
                        try {
                            Pageable pageable = PageRequest.of(0, 1);
                            List<com.njumarket.message.repository.projection.MessageContentProjection> lastMessages =
                                    messageRepository.findLastMessageContentForUser(
                                            message.getConversationId(), receiverId, pageable);

                            if (!lastMessages.isEmpty()) {
                                com.njumarket.message.repository.projection.MessageContentProjection newLastMessage = lastMessages.get(0);
                                conversation.setLastMessageForUser(receiverId,
                                        newLastMessage.getContent(),
                                        newLastMessage.getCreatedAt());
                            } else {
                                conversation.setLastMessageForUser(receiverId, null, null);
                            }
                        } catch (Exception e) {
                            log.warn("删除消息时更新接收方最后消息失败: conversationId={}, messageId={}, error={}",
                                    message.getConversationId(), messageId, e.getMessage());
                        }
                    }
                }
            }

            conversationRepository.save(conversation);

            return Result.ok("删除消息成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("删除消息失败，请稍后重试", e);
        }
    }

    // ==================== 搜索消息 ====================

    @Override
    public Result searchMessages(String userId, String conversationId, String keyword, int page, int size) {
        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                throw new BusinessException("对话不存在");
            }

            Conversation conversation = convOpt.get();
            if (!conversation.involvesUser(userId)) {
                throw new BusinessException("无权访问此对话");
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Message> messagesPage = messageRepository.searchMessages(conversationId, keyword, pageable);

            Set<String> userIds = new HashSet<>();
            userIds.add(conversation.getUserId1());
            userIds.add(conversation.getUserId2());

            Map<String, UserProfileInternalDTO> profileMapTemp = userCacheService.getUserProfilesByIds(userIds);
            final Map<String, UserProfileInternalDTO> finalProfileMap = profileMapTemp;

            // 批量加载商品快照
            List<String> messageIds = messagesPage.getContent().stream()
                    .map(Message::getMessageId).collect(Collectors.toList());
            Map<String, CommoditySnapshot> snapshotMap = commoditySnapshotRepository
                    .findByMessageIdIn(messageIds).stream()
                    .collect(Collectors.toMap(CommoditySnapshot::getMessageId, s -> s));

            Page<MessageDTO> dtoPage = messagesPage.map(message ->
                    convertMessageToDTOWithMap(message, userId, finalProfileMap, snapshotMap));

            return Result.ok("搜索消息成功", dtoPage);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("搜索消息失败，请稍后重试", e);
        }
    }

    // ==================== 黑名单（占位实现） ====================

    @Override
    public Result blockUser(String userId, String blockedUserId, String reason) {
        return Result.ok("功能开发中");
    }

    @Override
    public Result unblockUser(String userId, String blockedUserId) {
        return Result.ok("功能开发中");
    }

    @Override
    public Result isBlocked(String userId, String otherUserId) {
        return Result.ok("检查成功", false);
    }

    // ==================== 私有辅助方法：会话恢复推送 ====================

    private void pushConversationRestoredEvent(Conversation conversation, String receiverId) {
        try {
            Set<String> userIds = new HashSet<>();
            userIds.add(conversation.getUserId1());
            userIds.add(conversation.getUserId2());

            Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
            Map<String, User> userMap = new HashMap<>();

            if (!userIds.isEmpty()) {
                profileMap = userCacheService.getUserProfilesByIds(userIds);
                Map<String, UserInternalDTO> userDTOMap = userCacheService.getUsersByIds(userIds);
                userMap = userDTOMap.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            UserInternalDTO dto = e.getValue();
                            User user = new User();
                            user.setUserId(dto.getUserId());
                            user.setUsername(dto.getUsername());
                            user.setPrimaryPhone(dto.getPrimaryPhone());
                            user.setAccountStatus(dto.getAccountStatus());
                            user.setRegisterTime(dto.getRegisterTime());
                            return user;
                        }
                ));
            }

            ConversationDTO conversationDTO = buildConversationDTOWithMap(conversation, receiverId, profileMap, userMap);
            conversationDTO.setMessages(null);
            conversationDTO.setTotalMessages(null);

            Map<String, Object> conversationRestore = new java.util.HashMap<>();
            conversationRestore.put("type", "CONVERSATION_VISIBILITY_RESTORED");
            conversationRestore.put("conversation", conversationDTO);
            conversationRestore.put("conversationId", conversation.getConversationId());
            conversationRestore.put("timestamp", LocalDateTime.now().toString());

            String eventId = "conversation_restored_" + conversation.getConversationId() + "_" + System.currentTimeMillis();
            conversationRestore.put("messageId", eventId);
            messagePushEventProducer.sendMessagePushEvent(receiverId, eventId, conversationRestore, "CONVERSATION_RESTORED");
            log.debug("会话恢复通知推送任务已发送到MQ: receiverId={}, conversationId={}",
                    receiverId, conversation.getConversationId());
        } catch (Exception e) {
            log.warn("发送会话恢复通知推送任务到MQ失败: receiverId={}, conversationId={}, error={}",
                    receiverId, conversation.getConversationId(), e.getMessage());
        }
    }

    // ==================== 私有辅助方法：DTO 转换 ====================

    private ConversationDTO buildConversationDTOWithMap(Conversation conversation, String currentUserId,
                                                        Map<String, UserProfileInternalDTO> profileMap,
                                                        Map<String, User> userMap) {
        if (profileMap == null) profileMap = new HashMap<>();
        if (userMap == null) userMap = new HashMap<>();

        ConversationDTO dto = new ConversationDTO();
        dto.setConversationId(conversation.getConversationId());
        dto.setLastMessageContent(conversation.getLastMessageContentForUser(currentUserId));
        dto.setLastMessageTime(conversation.getLastMessageTimeForUser(currentUserId));
        dto.setStatus(conversation.getStatus());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());

        Integer unreadCount = conversation.getUnreadCountForUser(currentUserId);
        dto.setUnreadCount(unreadCount != null ? unreadCount : 0);

        String otherUserId = conversation.getOtherUserId(currentUserId);
        dto.setOtherUserId(otherUserId);

        if (otherUserId != null) {
            UserProfileInternalDTO profile = profileMap.get(otherUserId);
            if (profile != null) {
                dto.setOtherUserNickname(profile.getNickname());
                dto.setOtherUserAvatar(profile.getAvatar());
            }

            User user = userMap.get(otherUserId);
            dto.setOtherUserIsDeleted(user == null || "DELETED".equals(user.getAccountStatus()));
        }

        if (conversation.getUserId1() != null) {
            UserProfileInternalDTO user1Profile = profileMap.get(conversation.getUserId1());
            if (user1Profile != null) {
                dto.setBuyerNickname(user1Profile.getNickname());
                dto.setBuyerAvatar(user1Profile.getAvatar());
            }
        }

        if (conversation.getUserId2() != null) {
            UserProfileInternalDTO user2Profile = profileMap.get(conversation.getUserId2());
            if (user2Profile != null) {
                dto.setSellerNickname(user2Profile.getNickname());
                dto.setSellerAvatar(user2Profile.getAvatar());
            }
        }

        return dto;
    }

    private MessageDTO convertMessageToDTOWithMap(Message message, String currentUserId,
                                                   Map<String, UserProfileInternalDTO> profileMap,
                                                   Map<String, CommoditySnapshot> snapshotMap) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setMessageType(message.getMessageType());
        dto.setContent(message.getContent());
        dto.setImageUrl(message.getImageUrl());
        dto.setCommodityId(message.getCommodityId());
        dto.setOrderId(message.getOrderId());
        dto.setIsRead(message.getIsRead());
        dto.setReadTime(message.getReadTime());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setIsMine(message.getSenderId().equals(currentUserId));

        UserProfileInternalDTO senderProfile = profileMap.get(message.getSenderId());
        if (senderProfile != null) {
            dto.setSenderNickname(senderProfile.getNickname());
            dto.setSenderAvatar(senderProfile.getAvatar());
        }

        // 填充商品快照
        if (snapshotMap != null) {
            CommoditySnapshot snapshot = snapshotMap.get(message.getMessageId());
            if (snapshot != null) {
                dto.setCommoditySnapshot(toSnapshotDTO(snapshot));
            }
        }

        return dto;
    }

    private MessageDTO convertMessageToDTO(Message message, String currentUserId, CommoditySnapshot snapshot) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setMessageType(message.getMessageType());
        dto.setContent(message.getContent());
        dto.setImageUrl(message.getImageUrl());
        dto.setCommodityId(message.getCommodityId());
        dto.setOrderId(message.getOrderId());
        dto.setIsRead(message.getIsRead());
        dto.setReadTime(message.getReadTime());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setIsMine(message.getSenderId().equals(currentUserId));

        if (snapshot != null) {
            dto.setCommoditySnapshot(toSnapshotDTO(snapshot));
        }

        Map<String, UserProfileInternalDTO> senderProfileMap =
                userCacheService.getUserProfilesByIds(java.util.Collections.singletonList(message.getSenderId()));
        UserProfileInternalDTO senderProfile = senderProfileMap.get(message.getSenderId());
        if (senderProfile != null) {
            dto.setSenderNickname(senderProfile.getNickname());
            dto.setSenderAvatar(senderProfile.getAvatar());
        }

        return dto;
    }

    private MessageDTO.CommoditySnapshotDTO toSnapshotDTO(CommoditySnapshot snapshot) {
        MessageDTO.CommoditySnapshotDTO dto = new MessageDTO.CommoditySnapshotDTO();
        dto.setCommodityId(snapshot.getCommodityId());
        dto.setTitle(snapshot.getTitle());
        dto.setPrice(snapshot.getPrice());
        dto.setImageUrl(snapshot.getImageUrl());
        dto.setStatus(snapshot.getStatus());
        return dto;
    }

    private String extractFirstImage(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) return null;
        try {
            List<String> images = objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
            return (images != null && !images.isEmpty()) ? images.get(0) : null;
        } catch (Exception e) {
            log.debug("解析商品图片JSON失败，尝试直接返回: {}", imagesJson);
            return imagesJson.startsWith("[") ? null : imagesJson;
        }
    }
}
