package com.njumarket.commodity.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.njumarket.commodity.entity.Commodity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 商品搜索索引文档
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)  // 确保所有字段都被序列化，包括 null 值
@Document(indexName = "commodities")
@Setting(settingPath = "/elasticsearch/commodity-settings.json")
public class CommoditySearchDocument {

    @Id
    private String commodityId;

    @Field(type = FieldType.Keyword)
    private String sellerId;

    @Field(type = FieldType.Text, analyzer = "zh_max", searchAnalyzer = "zh_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "zh_max", searchAnalyzer = "zh_smart")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Keyword)
    private String conditionLevel;

    @Field(type = FieldType.Keyword)
    private String commodityStatus;

    @Field(type = FieldType.Keyword)
    private String buyerVisibility;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Long)
    private Long clickCount;

    @Field(type = FieldType.Text, analyzer = "zh_max", searchAnalyzer = "zh_smart")
    private String addressSnapshotFull;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime publishTime;

    @Field(type = FieldType.Text, analyzer = "zh_max", searchAnalyzer = "zh_smart")
    private String keywordPayload;

    public static CommoditySearchDocument fromCommodity(Commodity commodity) {
        CommoditySearchDocument document = new CommoditySearchDocument();
        document.setCommodityId(commodity.getCommodityId());
        document.setSellerId(commodity.getSellerId());
        document.setTitle(commodity.getTitle());
        document.setDescription(Optional.ofNullable(commodity.getDescription()).orElse(""));
        document.setCategory(commodity.getCategory());
        document.setPrice(commodity.getPrice());
        document.setConditionLevel(commodity.getConditionLevel());
        document.setCommodityStatus(commodity.getCommodityStatus());
        document.setBuyerVisibility(commodity.getBuyerVisibility());
        document.setStock(commodity.getStock());
        document.setClickCount(Optional.ofNullable(commodity.getClickCount()).map(Integer::longValue).orElse(0L));
        document.setAddressSnapshotFull(Optional.ofNullable(commodity.getAddressSnapshotFull()).orElse(commodity.getLocation()));
        document.setLocation(commodity.getLocation());
        // 确保 publishTime 截断到秒级精度（去除纳秒部分）
        if (commodity.getPublishTime() != null) {
            document.setPublishTime(commodity.getPublishTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        } else {
            document.setPublishTime(null);
        }

        String keywordPayload = String.join(" ",
                Optional.ofNullable(commodity.getTitle()).orElse(""),
                Optional.ofNullable(commodity.getDescription()).orElse(""),
                Optional.ofNullable(commodity.getCategory()).orElse(""),
                Optional.ofNullable(commodity.getConditionLevel()).orElse(""),
                Optional.ofNullable(commodity.getAddressSnapshotProvince()).orElse(""),
                Optional.ofNullable(commodity.getAddressSnapshotCity()).orElse(""),
                Optional.ofNullable(commodity.getAddressSnapshotDistrict()).orElse(""),
                Optional.ofNullable(commodity.getAddressSnapshotFull()).orElse("")
        );
        document.setKeywordPayload(keywordPayload);
        return document;
    }

    /**
     * 从商品实体构建搜索文档，并可选用 AI 丰度增强后的 keywordPayload。
     * 当 enrichedKeywordPayload 非空时，将其与原标题等合并作为 keywordPayload，否则与 {@link #fromCommodity(Commodity)} 一致。
     */
    public static CommoditySearchDocument fromCommodity(Commodity commodity, String enrichedKeywordPayload) {
        CommoditySearchDocument document = fromCommodity(commodity);
        if (enrichedKeywordPayload != null && !enrichedKeywordPayload.isBlank()) {
            String base = String.join(" ",
                    Optional.ofNullable(commodity.getTitle()).orElse(""),
                    Optional.ofNullable(commodity.getCategory()).orElse(""),
                    Optional.ofNullable(commodity.getAddressSnapshotFull()).orElse("")
            );
            document.setKeywordPayload(base + " " + enrichedKeywordPayload.trim());
        }
        return document;
    }
}

