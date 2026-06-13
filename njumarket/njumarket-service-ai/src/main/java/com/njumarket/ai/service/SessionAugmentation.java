package com.njumarket.ai.service;

import org.springframework.util.StringUtils;

/**
 * 本轮请求的<strong>辅助</strong>系统上下文（Redis 画像摘要、Milvus 语义召回），
 * 通过 {@link dev.langchain4j.service.AiServices} 的 systemMessageProvider 拼入，不写入 {@link dev.langchain4j.memory.ChatMemory}。
 */
final class SessionAugmentation {

    private final String redisProfileSummary;
    private final String conversationSemanticRecall;
    private final String userProfileSemanticRecall;

    SessionAugmentation(String redisProfileSummary,
                        String conversationSemanticRecall,
                        String userProfileSemanticRecall) {
        this.redisProfileSummary = redisProfileSummary;
        this.conversationSemanticRecall = conversationSemanticRecall;
        this.userProfileSemanticRecall = userProfileSemanticRecall;
    }

    boolean isEmpty() {
        return !StringUtils.hasText(redisProfileSummary)
            && !StringUtils.hasText(conversationSemanticRecall)
            && !StringUtils.hasText(userProfileSemanticRecall);
    }

    String toAppendix() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(redisProfileSummary)) {
            sb.append("=== 用户画像（Redis 摘要，辅助）===\n")
                .append(redisProfileSummary)
                .append("\n请结合以上偏好提供个性化建议。\n");
        }
        if (StringUtils.hasText(conversationSemanticRecall)) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("=== 语义召回 · 与本问题相关的历史对话片段（辅助，非完整聊天记录）===\n")
                .append(conversationSemanticRecall).append("\n");
        }
        if (StringUtils.hasText(userProfileSemanticRecall)) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("=== 语义召回 · 与本问题相关的用户画像片段（辅助）===\n")
                .append(userProfileSemanticRecall).append("\n");
        }
        return sb.toString();
    }
}
