package com.njumarket.ai.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 会话与消息：PostgreSQL（权威）；用户画像摘要：Redis。
 */
@Component
@Primary
@RequiredArgsConstructor
public class HybridAIConversationStorage implements AIConversationStorage {

    private final JpaAIConversationStorage jpa;
    private final RedisProfileStorage redisProfile;

    @Override
    public ConvMeta createOrGetConversation(String conversationId, String userId, String title) {
        return jpa.createOrGetConversation(conversationId, userId, title);
    }

    @Override
    public void updateTitle(String conversationId, String title) {
        jpa.updateTitle(conversationId, title);
    }

    @Override
    public void incrementMessageCount(String conversationId, int increment) {
        jpa.incrementMessageCount(conversationId, increment);
    }

    @Override
    public List<ConvMeta> getUserConversations(String userId, int limit) {
        return jpa.getUserConversations(userId, limit);
    }

    @Override
    public void deleteConversation(String conversationId, String userId) {
        jpa.deleteConversation(conversationId, userId);
    }

    @Override
    public Optional<MessageRecord> getLatestMessage(String conversationId) {
        return jpa.getLatestMessage(conversationId);
    }

    @Override
    public List<MessageRecord> getMessages(String conversationId, String userId, int limit) {
        return jpa.getMessages(conversationId, userId, limit);
    }

    @Override
    public MessageRecord appendMessage(String conversationId, String userId, String role, String content, String recommendedCommodityIds) {
        return jpa.appendMessage(conversationId, userId, role, content, recommendedCommodityIds);
    }

    @Override
    public long countMessages(String conversationId) {
        return jpa.countMessages(conversationId);
    }

    @Override
    public List<MessageRecord> getRecentMessages(String conversationId, int limit) {
        return jpa.getRecentMessages(conversationId, limit);
    }

    @Override
    public void saveProfileSummary(String userId, String summary) {
        redisProfile.saveProfileSummary(userId, summary);
    }

    @Override
    public Optional<ProfileSummary> getProfileSummary(String userId) {
        return redisProfile.getProfileSummary(userId);
    }

    @Override
    public Optional<ConversationMemorySnapshot> getConversationMemorySnapshot(String conversationId) {
        return jpa.getConversationMemorySnapshot(conversationId);
    }

    @Override
    public void updateConversationMemorySnapshot(String conversationId, String memorySummary, int windowMessageCount) {
        jpa.updateConversationMemorySnapshot(conversationId, memorySummary, windowMessageCount);
    }
}
