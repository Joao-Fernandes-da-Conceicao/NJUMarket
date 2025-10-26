package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.ConversationDTO;
import com.njumarket.njumarket.dto.MessageDTO;
import com.njumarket.njumarket.dto.SendMessageRequest;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.entity.*;
import com.njumarket.njumarket.repository.*;
import com.njumarket.njumarket.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContactServiceImpl implements ContactService {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    // @Autowired
    // private CommodityRepository commodityRepository;
    
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
                if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
                    return Result.fail("无权访问此对话");
                }
            } else {
                // 创建新对话
                conversation = new Conversation();
                conversation.setBuyerId(userId);
                conversation.setSellerId(request.getReceiverId());
                conversation.setStatus("ACTIVE");
                conversationRepository.save(conversation);
            }
            
            // 创建消息
            Message message = new Message();
            message.setConversationId(conversation.getConversationId());
            message.setSenderId(userId);
            message.setReceiverId(request.getReceiverId());
            message.setMessageType(request.getMessageType() != null ? request.getMessageType() : "TEXT");
            message.setContent(request.getContent());
            message.setImageUrl(request.getImageUrl());
            message.setIsRead(false);
            message.setIsDeleted(false);
            messageRepository.save(message);
            
            // 更新对话最后消息
            conversation.setLastMessageContent(request.getContent());
            conversation.setLastMessageTime(LocalDateTime.now());
            conversation.incrementUnreadForUser(request.getReceiverId());
            conversationRepository.save(conversation);
            
            // 转换为DTO返回
            MessageDTO messageDTO = convertMessageToDTO(message, userId);
            
            return Result.ok("消息发送成功", messageDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("发送消息失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result getConversations(String userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Conversation> conversationsPage = conversationRepository.findByUserId(userId, pageable);
            
            Page<ConversationDTO> dtoPage = conversationsPage.map(conversation -> {
                try {
                    return convertConversationToDTO(conversation, userId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            });
            
            return Result.ok("获取对话列表成功", dtoPage);
        } catch (Exception e) {
            e.printStackTrace();
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
            
            // 验证权限
            if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
                return Result.fail("无权访问此对话");
            }
            
            // 查询消息列表
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> messagesPage = messageRepository.findByConversationId(conversationId, pageable);
            
            // 转换对话和消息为DTO
            ConversationDTO dto = convertConversationToDTO(conversation, userId);
            
            List<MessageDTO> messageDTOs = new ArrayList<>();
            for (Message message : messagesPage.getContent()) {
                messageDTOs.add(convertMessageToDTO(message, userId));
            }
            dto.setMessages(messageDTOs);
            dto.setTotalMessages((int) messagesPage.getTotalElements());
            
            return Result.ok("获取对话详情成功", dto);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("获取对话详情失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result getOrCreateConversation(String userId, String otherUserId, 
                                         String commodityId, String orderId) {
        try {
            // 查找现有对话
            Optional<Conversation> existingConv;
            if (commodityId != null) {
                existingConv = conversationRepository.findByCommodityAndUsers(commodityId, userId, otherUserId);
            } else {
                existingConv = conversationRepository.findByBuyerIdAndSellerId(userId, otherUserId);
            }
            
            Conversation conversation;
            if (existingConv.isPresent()) {
                conversation = existingConv.get();
            } else {
                // 创建新对话
                conversation = new Conversation();
                conversation.setBuyerId(userId);
                conversation.setSellerId(otherUserId);
                conversation.setCommodityId(commodityId);
                conversation.setOrderId(orderId);
                conversation.setStatus("ACTIVE");
                conversationRepository.save(conversation);
            }
            
            ConversationDTO dto = convertConversationToDTO(conversation, userId);
            return Result.ok("获取对话成功", dto);
        } catch (Exception e) {
            e.printStackTrace();
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
            
            // 验证权限
            if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
                return Result.fail("无权访问此对话");
            }
            
            // 标记对话为已读
            conversation.markAsReadForUser(userId);
            conversationRepository.save(conversation);
            
            // 标记所有消息为已读
            messageRepository.markMessagesAsRead(conversationId, userId, LocalDateTime.now());
            
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
            if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
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
            if (!message.getSenderId().equals(userId)) {
                return Result.fail("只能删除自己发送的消息");
            }
            
            messageRepository.softDelete(messageId);
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
            if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
                return Result.fail("无权访问此对话");
            }
            
            // 搜索消息
            Pageable pageable = PageRequest.of(page, size);
            Page<Message> messagesPage = messageRepository.searchMessages(conversationId, keyword, pageable);
            
            Page<MessageDTO> dtoPage = messagesPage.map(message -> convertMessageToDTO(message, userId));
            
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
    
    // 辅助方法：转换Conversation为DTO
    private ConversationDTO convertConversationToDTO(Conversation conversation, String currentUserId) {
        ConversationDTO dto = new ConversationDTO();
        dto.setConversationId(conversation.getConversationId());
        dto.setBuyerId(conversation.getBuyerId());
        dto.setSellerId(conversation.getSellerId());
        dto.setCommodityId(conversation.getCommodityId());
        dto.setOrderId(conversation.getOrderId());
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
        
        // 查询对方用户信息
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
        
        // 查询买家信息
        Optional<UserProfile> buyerProfileOpt = userProfileRepository.findByUserId(conversation.getBuyerId());
        if (buyerProfileOpt.isPresent()) {
            UserProfile buyerProfile = buyerProfileOpt.get();
            dto.setBuyerNickname(buyerProfile.getNickname());
            dto.setBuyerAvatar(buyerProfile.getAvatar());
        }
        
        // 查询卖家信息
        Optional<UserProfile> sellerProfileOpt = userProfileRepository.findByUserId(conversation.getSellerId());
        if (sellerProfileOpt.isPresent()) {
            UserProfile sellerProfile = sellerProfileOpt.get();
            dto.setSellerNickname(sellerProfile.getNickname());
            dto.setSellerAvatar(sellerProfile.getAvatar());
        }
        
        return dto;
    }
    
    // 辅助方法：转换Message为DTO
    private MessageDTO convertMessageToDTO(Message message, String currentUserId) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setMessageType(message.getMessageType());
        dto.setContent(message.getContent());
        dto.setImageUrl(message.getImageUrl());
        dto.setCommoditySnapshot(message.getCommoditySnapshot());
        dto.setOrderSnapshot(message.getOrderSnapshot());
        dto.setIsRead(message.getIsRead());
        dto.setReadTime(message.getReadTime());
        dto.setCreatedAt(message.getCreatedAt());
        
        // 设置是否是当前用户发送的消息
        dto.setIsMine(message.getSenderId().equals(currentUserId));
        
        // 查询发送者信息
        Optional<UserProfile> senderProfileOpt = userProfileRepository.findByUserId(message.getSenderId());
        if (senderProfileOpt.isPresent()) {
            UserProfile senderProfile = senderProfileOpt.get();
            dto.setSenderNickname(senderProfile.getNickname());
            dto.setSenderAvatar(senderProfile.getAvatar());
        }
        
        return dto;
    }
}