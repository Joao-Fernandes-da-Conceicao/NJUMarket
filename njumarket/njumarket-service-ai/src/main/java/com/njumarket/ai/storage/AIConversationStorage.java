package com.njumarket.ai.storage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI 会话与消息存储接口。
 * <p>默认实现 {@link HybridAIConversationStorage}：会话与消息存 PostgreSQL（权威），用户画像摘要存 Redis；Milvus 由业务层写入向量。
 */
public interface AIConversationStorage {

    String KEY_CONV = "ai:conv:";
    String KEY_USER_CONVS = "ai:user:%s:convs";
    String KEY_CONV_MSGS = "ai:conv:%s:msgs";
    String KEY_PROFILE = "ai:profile:";

    /** 创建或获取会话 */
    ConvMeta createOrGetConversation(String conversationId, String userId, String title);

    void updateTitle(String conversationId, String title);

    void incrementMessageCount(String conversationId, int increment);

    List<ConvMeta> getUserConversations(String userId, int limit);

    void deleteConversation(String conversationId, String userId);

    Optional<MessageRecord> getLatestMessage(String conversationId);

    List<MessageRecord> getMessages(String conversationId, String userId, int limit);

    /** 追加消息（user 或 assistant），返回消息记录 */
    MessageRecord appendMessage(String conversationId, String userId, String role, String content, String recommendedCommodityIds);

    long countMessages(String conversationId);

    /** 获取最近 N 条消息（用于画像更新） */
    List<MessageRecord> getRecentMessages(String conversationId, int limit);

    /** 用户画像 */
    void saveProfileSummary(String userId, String summary);

    Optional<ProfileSummary> getProfileSummary(String userId);

    /**
     * 会话级 ChatMemory 快照：用于跨进程恢复窗口与在相同阈值触发归纳。
     *
     * @param memorySummary 【历史摘要】正文（不含「【历史摘要】」前缀），无则为 null
     * @param windowMessageCount 当前窗口内 user+assistant 消息条数（不含摘要 system 行）
     */
    record ConversationMemorySnapshot(String memorySummary, int windowMessageCount) {}

    Optional<ConversationMemorySnapshot> getConversationMemorySnapshot(String conversationId);

    void updateConversationMemorySnapshot(String conversationId, String memorySummary, int windowMessageCount);

    record ConvMeta(String conversationId, String userId, String title, int messageCount,
                    String status, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    record MessageRecord(String messageId, String conversationId, String userId, String role,
                         String content, String recommendedCommodityIds, LocalDateTime createdAt) {}

    record ProfileSummary(String userId, String profileSummary, LocalDateTime updatedAt) {}
}
