package com.njumarket.ai.storage;

import com.njumarket.ai.entity.AIConversationEntity;
import com.njumarket.ai.entity.AIMessageEntity;
import com.njumarket.ai.repository.AIConversationEntityRepository;
import com.njumarket.ai.repository.AIMessageEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI 会话与消息的关系库实现（权威来源）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaAIConversationStorage {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";

    private final AIConversationEntityRepository conversationRepo;
    private final AIMessageEntityRepository messageRepo;

    @Transactional
    public AIConversationStorage.ConvMeta createOrGetConversation(String conversationId, String userId, String title) {
        Optional<AIConversationEntity> opt = conversationRepo.findById(conversationId);
        if (opt.isPresent()) {
            return toConvMeta(opt.get());
        }
        LocalDateTime now = LocalDateTime.now();
        AIConversationEntity e = new AIConversationEntity();
        e.setConversationId(conversationId);
        e.setUserId(userId);
        e.setTitle(StringUtils.hasText(title) ? title : "新对话");
        e.setMessageCount(0);
        e.setWindowMessageCount(0);
        e.setMemorySummary(null);
        e.setStatus(STATUS_ACTIVE);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        conversationRepo.save(e);
        log.info("创建 AI 会话(关系库): conversationId={}, userId={}", conversationId, userId);
        return toConvMeta(e);
    }

    @Transactional
    public void updateTitle(String conversationId, String title) {
        if (!StringUtils.hasText(conversationId)) return;
        conversationRepo.findById(conversationId).ifPresent(e -> {
            if (title != null && title.length() > 200) {
                e.setTitle(title.substring(0, 200));
            } else {
                e.setTitle(title);
            }
            e.setUpdatedAt(LocalDateTime.now());
            conversationRepo.save(e);
        });
    }

    @Transactional
    public void incrementMessageCount(String conversationId, int increment) {
        if (!StringUtils.hasText(conversationId) || increment <= 0) return;
        conversationRepo.findById(conversationId).ifPresent(e -> {
            int mc = e.getMessageCount() != null ? e.getMessageCount() : 0;
            e.setMessageCount(mc + increment);
            e.setUpdatedAt(LocalDateTime.now());
            conversationRepo.save(e);
        });
    }

    public List<AIConversationStorage.ConvMeta> getUserConversations(String userId, int limit) {
        if (!StringUtils.hasText(userId)) return List.of();
        List<AIConversationEntity> list = conversationRepo.findActiveByUserIdOrderByUpdatedAtDesc(userId);
        if (list.size() > limit) {
            list = list.subList(0, limit);
        }
        return list.stream().map(this::toConvMeta).toList();
    }

    @Transactional
    public void deleteConversation(String conversationId, String userId) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(userId)) return;
        conversationRepo.findById(conversationId).ifPresent(e -> {
            if (userId.equals(e.getUserId())) {
                e.setStatus(STATUS_DELETED);
                e.setUpdatedAt(LocalDateTime.now());
                conversationRepo.save(e);
                log.info("删除 AI 会话(软删): conversationId={}", conversationId);
            }
        });
    }

    public Optional<AIConversationStorage.MessageRecord> getLatestMessage(String conversationId) {
        if (!StringUtils.hasText(conversationId)) return Optional.empty();
        return messageRepo.findFirstByConversationIdOrderByCreatedAtDesc(conversationId).map(this::toRecord);
    }

    public List<AIConversationStorage.MessageRecord> getMessages(String conversationId, String userId, int limit) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(userId)) return List.of();
        List<AIMessageEntity> desc = limit > 100
                ? messageRepo.findTop500ByConversationIdOrderByCreatedAtDesc(conversationId)
                : messageRepo.findTop100ByConversationIdOrderByCreatedAtDesc(conversationId);
        if (desc.isEmpty()) return List.of();
        Collections.reverse(desc);
        if (desc.size() > limit) {
            desc = desc.subList(desc.size() - limit, desc.size());
        }
        return desc.stream().map(this::toRecord).toList();
    }

    @Transactional
    public AIConversationStorage.MessageRecord appendMessage(String conversationId, String userId, String role,
                                                              String content, String recommendedCommodityIds) {
        String messageId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        AIMessageEntity m = new AIMessageEntity();
        m.setMessageId(messageId);
        m.setConversationId(conversationId);
        m.setUserId(userId);
        m.setRole(role);
        m.setContent(content);
        m.setRecommendedCommodityIds(recommendedCommodityIds);
        m.setCreatedAt(now);
        messageRepo.save(m);
        return new AIConversationStorage.MessageRecord(messageId, conversationId, userId, role, content,
                recommendedCommodityIds, now);
    }

    public long countMessages(String conversationId) {
        if (!StringUtils.hasText(conversationId)) return 0;
        return messageRepo.countByConversationId(conversationId);
    }

    public Optional<AIConversationStorage.ConversationMemorySnapshot> getConversationMemorySnapshot(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return Optional.empty();
        }
        return conversationRepo.findById(conversationId).map(e -> new AIConversationStorage.ConversationMemorySnapshot(
                e.getMemorySummary(),
                e.getWindowMessageCount() != null ? e.getWindowMessageCount() : 0
        ));
    }

    @Transactional
    public void updateConversationMemorySnapshot(String conversationId, String memorySummary, int windowMessageCount) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        conversationRepo.findById(conversationId).ifPresent(e -> {
            e.setMemorySummary(memorySummary);
            e.setWindowMessageCount(windowMessageCount);
            e.setUpdatedAt(LocalDateTime.now());
            conversationRepo.save(e);
            log.debug("已更新会话窗口快照: cid={}, windowCount={}", conversationId, windowMessageCount);
        });
    }

    public List<AIConversationStorage.MessageRecord> getRecentMessages(String conversationId, int limit) {
        if (!StringUtils.hasText(conversationId)) return List.of();
        List<AIMessageEntity> desc = messageRepo.findTop30ByConversationIdOrderByCreatedAtDesc(conversationId);
        if (desc.isEmpty()) return List.of();
        Collections.reverse(desc);
        if (desc.size() > limit) {
            desc = new ArrayList<>(desc.subList(desc.size() - limit, desc.size()));
        }
        return desc.stream().map(this::toRecord).toList();
    }

    private AIConversationStorage.ConvMeta toConvMeta(AIConversationEntity e) {
        return new AIConversationStorage.ConvMeta(
                e.getConversationId(),
                e.getUserId(),
                e.getTitle() != null ? e.getTitle() : "",
                e.getMessageCount() != null ? e.getMessageCount() : 0,
                e.getStatus() != null ? e.getStatus() : STATUS_ACTIVE,
                e.getCreatedAt() != null ? e.getCreatedAt() : LocalDateTime.now(),
                e.getUpdatedAt() != null ? e.getUpdatedAt() : LocalDateTime.now()
        );
    }

    private AIConversationStorage.MessageRecord toRecord(AIMessageEntity m) {
        return new AIConversationStorage.MessageRecord(
                m.getMessageId(),
                m.getConversationId(),
                m.getUserId(),
                m.getRole(),
                m.getContent(),
                m.getRecommendedCommodityIds(),
                m.getCreatedAt()
        );
    }
}
