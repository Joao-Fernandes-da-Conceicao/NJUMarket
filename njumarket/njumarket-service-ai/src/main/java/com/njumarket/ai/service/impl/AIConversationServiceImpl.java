package com.njumarket.ai.service.impl;

import com.njumarket.ai.service.AIConversationService;
import com.njumarket.ai.storage.AIConversationStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AIConversationServiceImpl implements AIConversationService {

    private final AIConversationStorage storage;

    @Override
    public AIConversationStorage.ConvMeta createOrGetConversation(String conversationId, String userId, String title) {
        return storage.createOrGetConversation(conversationId, userId, title);
    }

    @Override
    public void updateTitle(String conversationId, String title) {
        storage.updateTitle(conversationId, title);
    }

    @Override
    public void incrementMessageCount(String conversationId, int increment) {
        storage.incrementMessageCount(conversationId, increment);
    }

    @Override
    public List<AIConversationStorage.ConvMeta> getUserConversations(String userId, int limit) {
        return storage.getUserConversations(userId, limit);
    }

    @Override
    public void deleteConversation(String conversationId, String userId) {
        storage.deleteConversation(conversationId, userId);
    }

    @Override
    public Optional<AIConversationStorage.MessageRecord> getLatestMessage(String conversationId) {
        if (!StringUtils.hasText(conversationId)) return Optional.empty();
        return storage.getLatestMessage(conversationId);
    }

    @Override
    public List<AIConversationStorage.MessageRecord> getMessages(String conversationId, String userId) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(userId)) return List.of();
        return storage.getMessages(conversationId, userId, 100);
    }
}
