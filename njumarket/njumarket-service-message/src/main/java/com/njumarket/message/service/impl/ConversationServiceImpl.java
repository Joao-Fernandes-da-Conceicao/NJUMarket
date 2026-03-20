package com.njumarket.message.service.impl;

import com.njumarket.message.dto.ConversationDTO;
import com.njumarket.message.dto.MessageDTO;
import com.njumarket.message.entity.Conversation;
import com.njumarket.message.entity.CommoditySnapshot;
import com.njumarket.message.entity.Message;
import com.njumarket.message.entity.User;
import com.njumarket.message.repository.CommoditySnapshotRepository;
import com.njumarket.message.repository.ConversationRepository;
import com.njumarket.message.repository.MessageRepository;
import com.njumarket.message.service.ConversationService;
import com.njumarket.message.service.UserCacheService;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.njumarket.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CommoditySnapshotRepository commoditySnapshotRepository;
    private final UserCacheService userCacheService;
    private final com.njumarket.message.mq.MessagePushEventProducer messagePushEventProducer;

    // ==================== 对话列表 ====================

    @Override
    public Result getConversations(String userId, int page, int size) {
        try {
            log.info("开始查询对话列表: userId={}, page={}, size={}", userId, page, size);

            List<Conversation> allUserConversations = conversationRepository.findByUserIdAndStatus(userId, "ACTIVE");
            log.info("调试查询（不过滤可见性）: userId={}, 总数={}", userId, allUserConversations.size());
            for (Conversation conv : allUserConversations) {
                log.info("调试对话详情: conversationId={}, userId1={}, userId2={}, user1Visibility={}, user2Visibility={}, status={}",
                    conv.getConversationId(), conv.getUserId1(), conv.getUserId2(),
                    conv.getUser1Visibility(), conv.getUser2Visibility(), conv.getStatus());
            }

            List<Conversation> conversationsAsUser1 = conversationRepository
                    .findByUserId1AndStatusOrderByUser1LastMessageTime(userId, "ACTIVE");
            log.info("查询用户作为userId1的对话（过滤可见性）: userId={}, 数量={}", userId, conversationsAsUser1.size());

            List<Conversation> conversationsAsUser2 = conversationRepository
                    .findByUserId2AndStatusOrderByUser2LastMessageTime(userId, "ACTIVE");
            log.info("查询用户作为userId2的对话（过滤可见性）: userId={}, 数量={}", userId, conversationsAsUser2.size());

            Map<String, Conversation> conversationMap = new LinkedHashMap<>();
            for (Conversation conv : conversationsAsUser1) {
                conversationMap.put(conv.getConversationId(), conv);
            }
            for (Conversation conv : conversationsAsUser2) {
                conversationMap.put(conv.getConversationId(), conv);
            }

            List<Conversation> allConversations = new ArrayList<>(conversationMap.values());
            log.info("合并后的对话总数: userId={}, 总数={}", userId, allConversations.size());

            allConversations.sort((c1, c2) -> {
                LocalDateTime time1 = c1.getLastMessageTimeForUser(userId);
                LocalDateTime time2 = c2.getLastMessageTimeForUser(userId);
                if (time1 == null && time2 == null) return 0;
                if (time1 == null) return 1;
                if (time2 == null) return -1;
                return time2.compareTo(time1);
            });

            int total = allConversations.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);
            List<Conversation> pagedConversations = fromIndex < total
                    ? allConversations.subList(fromIndex, toIndex)
                    : new ArrayList<>();
            log.info("分页后的对话数量: userId={}, total={}, page={}, size={}, pagedCount={}",
                userId, total, page, size, pagedConversations.size());

            Set<String> userIds = new HashSet<>();
            Set<String> userIdsForUserCheck = new HashSet<>();
            for (Conversation conv : pagedConversations) {
                userIds.add(conv.getUserId1());
                userIds.add(conv.getUserId2());
                userIdsForUserCheck.add(conv.getUserId1());
                userIdsForUserCheck.add(conv.getUserId2());
            }

            final Map<String, UserProfileInternalDTO> profileMap = userCacheService.getUserProfilesByIds(userIds);

            Map<String, UserInternalDTO> userDTOMap = userCacheService.getUsersByIds(userIdsForUserCheck);
            final Map<String, User> userMap = userDTOMap.entrySet().stream().collect(Collectors.toMap(
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

    // ==================== 对话详情 ====================

    @Override
    public Result getConversationDetail(String userId, String conversationId, int page, int size) {
        try {
            log.info("开始获取对话详情: userId={}, conversationId={}, page={}, size={}",
                userId, conversationId, page, size);

            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                log.warn("对话不存在: conversationId={}", conversationId);
                throw new BusinessException("对话不存在");
            }

            Conversation conversation = convOpt.get();
            log.debug("查询到对话: conversationId={}, userId1={}, userId2={}",
                conversation.getConversationId(), conversation.getUserId1(), conversation.getUserId2());

            if (!conversation.involvesUser(userId)) {
                log.warn("无权访问对话: userId={}, conversationId={}", userId, conversationId);
                throw new BusinessException("无权访问此对话");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> messagesPage = messageRepository.findByConversationId(conversationId, pageable);
            log.info("查询到消息: conversationId={}, 总数={}, 当前页数量={}",
                conversationId, messagesPage.getTotalElements(), messagesPage.getContent().size());

            Set<String> userIds = new HashSet<>();
            userIds.add(conversation.getUserId1());
            userIds.add(conversation.getUserId2());

            Map<String, UserProfileInternalDTO> profileMap = userCacheService.getUserProfilesByIds(userIds);
            log.debug("批量查询用户档案（Redis→Feign）: userIds={}, profileMap大小={}", userIds, profileMap.size());

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
            final Map<String, UserProfileInternalDTO> finalProfileMap = profileMap;

            // 批量加载商品快照（避免 N+1 查询）
            List<String> msgIds = messagesPage.getContent().stream()
                    .map(Message::getMessageId).collect(Collectors.toList());
            Map<String, CommoditySnapshot> snapshotMap = commoditySnapshotRepository
                    .findByMessageIdIn(msgIds).stream()
                    .collect(Collectors.toMap(CommoditySnapshot::getMessageId, s -> s));

            List<MessageDTO> messageDTOs = new ArrayList<>();
            for (int i = 0; i < messagesPage.getContent().size(); i++) {
                Message message = messagesPage.getContent().get(i);
                try {
                    boolean isDeletedByCurrentUser = false;
                    if (userId.equals(message.getSenderId()) && Boolean.TRUE.equals(message.getDeletedBySender())) {
                        isDeletedByCurrentUser = true;
                    } else if (userId.equals(message.getReceiverId()) && Boolean.TRUE.equals(message.getDeletedByReceiver())) {
                        isDeletedByCurrentUser = true;
                    }

                    if (!isDeletedByCurrentUser) {
                        MessageDTO messageDTO = convertMessageToDTOWithMap(message, userId, finalProfileMap, snapshotMap);
                        messageDTOs.add(messageDTO);
                    }
                } catch (Exception e) {
                    log.error("转换消息失败: messageId={}, conversationId={}, error={}",
                        message.getMessageId(), conversationId, e.getMessage(), e);
                }
            }

            log.info("消息转换完成: conversationId={}, 原始消息数={}, 转换后消息数={}",
                conversationId, messagesPage.getContent().size(), messageDTOs.size());

            dto.setMessages(messageDTOs);
            dto.setTotalMessages((int) messagesPage.getTotalElements());

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

    // ==================== 历史消息加载 ====================

    @Override
    public Result getMessagesBefore(String userId, String conversationId, String beforeTime, int size) {
        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                throw new BusinessException("对话不存在");
            }

            Conversation conversation = convOpt.get();

            if (!conversation.involvesUser(userId)) {
                throw new BusinessException("无权访问此对话");
            }

            LocalDateTime beforeDateTime;
            try {
                beforeDateTime = LocalDateTime.parse(beforeTime);
            } catch (Exception e) {
                throw new BusinessException("时间参数格式错误");
            }

            Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> messagesPage = messageRepository.findMessagesBefore(conversationId, beforeDateTime, pageable);

            Set<String> userIds = new HashSet<>();
            userIds.add(conversation.getUserId1());
            userIds.add(conversation.getUserId2());

            Map<String, UserProfileInternalDTO> profileMap = userCacheService.getUserProfilesByIds(userIds);
            final Map<String, UserProfileInternalDTO> finalProfileMap = profileMap;

            // 批量加载商品快照
            List<String> beforeMsgIds = messagesPage.getContent().stream()
                    .map(Message::getMessageId).collect(Collectors.toList());
            Map<String, CommoditySnapshot> beforeSnapshotMap = commoditySnapshotRepository
                    .findByMessageIdIn(beforeMsgIds).stream()
                    .collect(Collectors.toMap(CommoditySnapshot::getMessageId, s -> s));

            List<MessageDTO> messageDTOs = new ArrayList<>();
            for (Message message : messagesPage.getContent()) {
                boolean isDeletedByCurrentUser = false;
                if (userId.equals(message.getSenderId()) && Boolean.TRUE.equals(message.getDeletedBySender())) {
                    isDeletedByCurrentUser = true;
                } else if (userId.equals(message.getReceiverId()) && Boolean.TRUE.equals(message.getDeletedByReceiver())) {
                    isDeletedByCurrentUser = true;
                }

                if (!isDeletedByCurrentUser) {
                    messageDTOs.add(convertMessageToDTOWithMap(message, userId, finalProfileMap, beforeSnapshotMap));
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

    // ==================== 创建/获取对话 ====================

    @Override
    public Result getOrCreateConversation(String userId, String otherUserId) {
        try {
            Optional<Conversation> existingConv = conversationRepository.findByUserPairActive(userId, otherUserId);

            Conversation conversation;
            if (existingConv.isPresent()) {
                conversation = existingConv.get();
            } else {
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

    // ==================== 标记已读 ====================

    @Override
    public Result markConversationAsRead(String userId, String conversationId) {
        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                throw new BusinessException("对话不存在");
            }

            Conversation conversation = convOpt.get();

            if (!conversation.involvesUser(userId)) {
                throw new BusinessException("无权访问此对话");
            }

            conversation.markAsReadForUser(userId);
            conversationRepository.save(conversation);

            String otherUserId = conversation.getUserId1().equals(userId)
                    ? conversation.getUserId2()
                    : conversation.getUserId1();

            List<com.njumarket.message.repository.projection.MessageIdProjection> unreadMessageIds =
                    messageRepository.findUnreadMessageIdsByConversationAndReceiver(conversationId, userId);

            LocalDateTime readTime = LocalDateTime.now();
            messageRepository.markMessagesAsRead(conversationId, userId, readTime);

            if (unreadMessageIds != null && !unreadMessageIds.isEmpty()) {
                try {
                    Map<String, Object> readNotification = new java.util.HashMap<>();
                    readNotification.put("type", "MESSAGE_READ");
                    readNotification.put("conversationId", conversationId);
                    readNotification.put("readTime", readTime.toString());

                    List<String> readMessageIds = unreadMessageIds.stream()
                            .map(com.njumarket.message.repository.projection.MessageIdProjection::getMessageId)
                            .collect(java.util.stream.Collectors.toList());
                    readNotification.put("messageIds", readMessageIds);

                    try {
                        String messageReadMessageId = "message_read_" + conversationId + "_" + System.currentTimeMillis();
                        readNotification.put("messageId", messageReadMessageId);
                        messagePushEventProducer.sendMessagePushEvent(
                                otherUserId,
                                messageReadMessageId,
                                readNotification,
                                "MESSAGE_READ"
                        );
                        log.debug("已读通知推送任务已发送到MQ: otherUserId={}, conversationId={}",
                                otherUserId, conversationId);
                    } catch (Exception e) {
                        log.warn("发送已读通知推送任务到MQ失败: otherUserId={}, conversationId={}, error={}",
                                otherUserId, conversationId, e.getMessage());
                    }
                } catch (Exception e) {
                    log.warn("推送已读通知失败: senderId={}, conversationId={}, error={}",
                            otherUserId, conversationId, e.getMessage());
                }
            }

            try {
                Integer totalUnreadCount = conversationRepository.getTotalUnreadCount(userId);
                if (totalUnreadCount == null) {
                    totalUnreadCount = 0;
                }

                Integer conversationUnreadCount = conversation.getUnreadCountForUser(userId);
                if (conversationUnreadCount == null) {
                    conversationUnreadCount = 0;
                }

                Map<String, Object> unreadCountUpdate = new java.util.HashMap<>();
                unreadCountUpdate.put("type", "UNREAD_COUNT_UPDATE");
                unreadCountUpdate.put("unreadCount", totalUnreadCount);
                unreadCountUpdate.put("conversationId", conversationId);
                unreadCountUpdate.put("conversationUnreadCount", conversationUnreadCount);
                unreadCountUpdate.put("timestamp", LocalDateTime.now().toString());

                try {
                    messagePushEventProducer.sendMessagePushEvent(
                            userId,
                            "unread_count_" + userId + "_" + System.currentTimeMillis(),
                            unreadCountUpdate,
                            "UNREAD_COUNT_UPDATE"
                    );
                    log.debug("未读数更新推送任务已发送到MQ: userId={}, unreadCount={}", userId, totalUnreadCount);
                } catch (Exception e) {
                    log.warn("发送未读数更新推送任务到MQ失败: userId={}, error={}", userId, e.getMessage());
                }
            } catch (Exception e) {
                log.warn("推送未读数更新失败: userId={}, error={}", userId, e.getMessage());
            }

            return Result.ok("标记已读成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("标记已读失败，请稍后重试", e);
        }
    }

    // ==================== 未读数 ====================

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

    // ==================== 删除对话 ====================

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

            conversation.setVisibilityForUser(userId, false);
            conversationRepository.save(conversation);

            return Result.ok("删除对话成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("删除对话失败，请稍后重试", e);
        }
    }

    // ==================== 获取与特定用户的对话 ====================

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

    // ==================== 私有辅助方法：DTO 转换 ====================

    private ConversationDTO convertConversationToDTOWithMap(Conversation conversation, String currentUserId,
                                                             Map<String, UserProfileInternalDTO> profileMap,
                                                             Map<String, User> userMap) {
        if (conversation == null) throw new IllegalArgumentException("Conversation不能为null");
        if (currentUserId == null) throw new IllegalArgumentException("currentUserId不能为null");
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

    private ConversationDTO convertConversationToDTO(Conversation conversation, String currentUserId) {
        ConversationDTO dto = new ConversationDTO();
        dto.setConversationId(conversation.getConversationId());
        dto.setLastMessageContent(conversation.getLastMessageContentForUser(currentUserId));
        dto.setLastMessageTime(conversation.getLastMessageTimeForUser(currentUserId));
        dto.setStatus(conversation.getStatus());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        dto.setUnreadCount(conversation.getUnreadCountForUser(currentUserId));

        String otherUserId = conversation.getOtherUserId(currentUserId);
        dto.setOtherUserId(otherUserId);

        if (otherUserId != null) {
            Map<String, UserProfileInternalDTO> otherProfileMap =
                    userCacheService.getUserProfilesByIds(java.util.Collections.singletonList(otherUserId));
            UserProfileInternalDTO otherProfile = otherProfileMap.get(otherUserId);
            if (otherProfile != null) {
                dto.setOtherUserNickname(otherProfile.getNickname());
                dto.setOtherUserAvatar(otherProfile.getAvatar());
            }

            UserInternalDTO otherUserDTO = userCacheService.getUserById(otherUserId);
            boolean isDeleted = otherUserDTO == null || "DELETED".equals(otherUserDTO.getAccountStatus());
            dto.setOtherUserIsDeleted(isDeleted);
        }

        List<String> userIds = new ArrayList<>();
        userIds.add(conversation.getUserId1());
        userIds.add(conversation.getUserId2());
        Map<String, UserProfileInternalDTO> profileMap = userCacheService.getUserProfilesByIds(userIds);

        UserProfileInternalDTO user1Profile = profileMap.get(conversation.getUserId1());
        if (user1Profile != null) {
            dto.setBuyerNickname(user1Profile.getNickname());
            dto.setBuyerAvatar(user1Profile.getAvatar());
        }

        UserProfileInternalDTO user2Profile = profileMap.get(conversation.getUserId2());
        if (user2Profile != null) {
            dto.setSellerNickname(user2Profile.getNickname());
            dto.setSellerAvatar(user2Profile.getAvatar());
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
                MessageDTO.CommoditySnapshotDTO snapshotDTO = new MessageDTO.CommoditySnapshotDTO();
                snapshotDTO.setCommodityId(snapshot.getCommodityId());
                snapshotDTO.setTitle(snapshot.getTitle());
                snapshotDTO.setPrice(snapshot.getPrice());
                snapshotDTO.setImageUrl(snapshot.getImageUrl());
                snapshotDTO.setStatus(snapshot.getStatus());
                dto.setCommoditySnapshot(snapshotDTO);
            }
        }

        return dto;
    }
}
