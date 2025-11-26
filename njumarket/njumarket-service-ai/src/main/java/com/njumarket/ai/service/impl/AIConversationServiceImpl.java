package com.njumarket.ai.service.impl;

import com.njumarket.ai.entity.AIConversation;
import com.njumarket.ai.repository.AIConversationRepository;
import com.njumarket.ai.service.AIConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI 聊天会话服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIConversationServiceImpl implements AIConversationService {
    
    private final AIConversationRepository aiConversationRepository;
    
    @Override
    @Transactional
    public AIConversation createOrGetConversation(String conversationId, String userId, String title) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("conversationId 和 userId 不能为空");
        }
        
        Optional<AIConversation> existing = aiConversationRepository.findByConversationIdAndUserId(conversationId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        AIConversation conversation = new AIConversation();
        conversation.setConversationId(conversationId);
        conversation.setUserId(userId);
        conversation.setTitle(StringUtils.hasText(title) ? title : "新对话");
        conversation.setMessageCount(0);
        conversation.setStatus("ACTIVE");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        
        AIConversation saved = aiConversationRepository.save(conversation);
        log.info("创建 AI 会话: conversationId={}, userId={}, title={}", conversationId, userId, title);
        
        return saved;
    }
    
    @Override
    @Transactional
    public void updateTitle(String conversationId, String title) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        
        if (title != null && title.length() > 200) {
            title = title.substring(0, 200);
        }
        
        aiConversationRepository.updateTitle(conversationId, title);
        log.debug("更新会话标题: conversationId={}, title={}", conversationId, title);
    }
    
    @Override
    @Transactional
    public void incrementMessageCount(String conversationId, int increment) {
        if (!StringUtils.hasText(conversationId) || increment <= 0) {
            return;
        }
        
        aiConversationRepository.incrementMessageCount(conversationId, increment);
        log.debug("增加消息数量: conversationId={}, increment={}", conversationId, increment);
    }
    
    @Override
    @Transactional
    public void updateConversation(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        
        Optional<AIConversation> conversation = aiConversationRepository.findById(conversationId);
        if (conversation.isPresent()) {
            AIConversation conv = conversation.get();
            conv.setUpdatedAt(LocalDateTime.now());
            aiConversationRepository.save(conv);
        }
    }
    
    @Override
    public List<AIConversation> getUserConversations(String userId, int limit) {
        if (!StringUtils.hasText(userId)) {
            return List.of();
        }
        
        List<AIConversation> conversations = aiConversationRepository.findActiveByUserId(userId);
        
        if (conversations.size() > limit) {
            return conversations.subList(0, limit);
        }
        
        return conversations;
    }
    
    @Override
    public Optional<AIConversation> getConversation(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return Optional.empty();
        }
        
        return aiConversationRepository.findById(conversationId);
    }
    
    @Override
    @Transactional
    public void deleteConversation(String conversationId, String userId) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(userId)) {
            return;
        }
        
        Optional<AIConversation> conversation = aiConversationRepository.findByConversationIdAndUserId(conversationId, userId);
        if (conversation.isPresent()) {
            aiConversationRepository.updateStatus(conversationId, "DELETED");
            log.info("删除 AI 会话: conversationId={}, userId={}", conversationId, userId);
        }
    }
}

