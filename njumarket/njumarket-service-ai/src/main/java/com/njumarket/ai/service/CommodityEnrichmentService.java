package com.njumarket.ai.service;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 商品检索丰度增强服务：通过单次 Chat 为商品生成更丰富的可检索文本。
 * 用于索引重建/创建时，由商品服务通过 Feign 调用，将返回文本写入 ES 的 keywordPayload 等字段以提升搜索效果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommodityEnrichmentService {

    private static final String ENRICHMENT_SYSTEM = "你是一个二手商品检索优化助手。"
        + "根据给定的商品信息，生成一段「仅用于搜索引擎检索」的丰度文本。要求：\n"
        + "1. 保留原标题与描述中的关键词；\n"
        + "2. 补充同义词、常见说法、使用场景（例如：教材→课本、考研书、教科书）；\n"
        + "3. 若有成色、地区、品类信息，用自然词融入；\n"
        + "4. 输出为一段连贯的短文，不要列表、不要编号，总长度 80～200 字；\n"
        + "5. 只输出这段文本，不要任何前缀、解释或换行。";

    private final ChatModel chatModel;

    /**
     * 单次 Chat：根据商品摘要生成丰度更高的可检索文本。
     *
     * @param title       商品标题
     * @param description 商品描述（可为空）
     * @param category    品类
     * @param conditionLevel 成色
     * @param location    位置/地区摘要
     * @param addressSnapshotFull 完整地址快照（可为空）
     * @return 丰度文本，失败或为空时返回 empty
     */
    public Optional<String> enrichForSearch(String title,
                                            String description,
                                            String category,
                                            String conditionLevel,
                                            String location,
                                            String addressSnapshotFull) {
        if (!StringUtils.hasText(title)) {
            return Optional.empty();
        }
        StringBuilder userContent = new StringBuilder();
        userContent.append("商品标题：").append(title);
        if (StringUtils.hasText(description)) {
            String desc = description.length() > 500 ? description.substring(0, 500) + "…" : description;
            userContent.append("\n商品描述：").append(desc);
        }
        if (StringUtils.hasText(category)) {
            userContent.append("\n品类：").append(category);
        }
        if (StringUtils.hasText(conditionLevel)) {
            userContent.append("\n成色：").append(conditionLevel);
        }
        if (StringUtils.hasText(location)) {
            userContent.append("\n地区/位置：").append(location);
        }
        if (StringUtils.hasText(addressSnapshotFull)) {
            userContent.append("\n地址：").append(addressSnapshotFull);
        }
        userContent.append("\n\n请生成上述商品的检索丰度文本。");

        try {
            String fullPrompt = ENRICHMENT_SYSTEM + "\n\n" + userContent;
            ChatResponse response = chatModel.chat(UserMessage.from(fullPrompt));
            String text = response != null && response.aiMessage() != null
                ? response.aiMessage().text().trim()
                : null;
            if (StringUtils.hasText(text)) {
                return Optional.of(text);
            }
        } catch (Exception e) {
            log.warn("商品丰度增强单次 Chat 失败: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
