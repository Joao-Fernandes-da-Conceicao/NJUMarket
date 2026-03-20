package com.njumarket.ai.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.njumarket.ai.config.MilvusProperties;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorService {

    private static final String ID_FIELD = "id";
    private static final String VECTOR_FIELD = "embedding";
    private static final String CONTENT_FIELD = "content";
    private static final String BIZ_ID_FIELD = "bizId";

    private final MilvusClientV2 milvusClient;
    private final MilvusProperties milvusProperties;
    private final VectorEmbeddingService vectorEmbeddingService;

    @PostConstruct
    public void initCollections() {
        if (!milvusProperties.isEnabled()) {
            log.info("Milvus disabled by config, skip initialization");
            return;
        }
        ensureCollection(milvusProperties.getCommodityCollection());
        ensureCollection(milvusProperties.getUserProfileCollection());
        ensureCollection(milvusProperties.getConversationCollection());
        log.info("Milvus collections ready: commodity={}, userProfile={}",
                milvusProperties.getCommodityCollection(), milvusProperties.getUserProfileCollection());
    }

    public void upsertCommodityVector(String id, String bizId, String content, List<Float> embedding) {
        upsert(milvusProperties.getCommodityCollection(), id, bizId, content, normalizeDimension(embedding));
    }

    public void upsertUserProfileVector(String id, String bizId, String content, List<Float> embedding) {
        upsert(milvusProperties.getUserProfileCollection(), id, bizId, content, normalizeDimension(embedding));
    }

    public void upsertCommodityVectorByText(String id, String bizId, String content) {
        upsertCommodityVector(id, bizId, content, vectorEmbeddingService.embedText(content));
    }

    public void upsertUserProfileVectorByText(String id, String bizId, String content) {
        upsertUserProfileVector(id, bizId, content, vectorEmbeddingService.embedText(content));
    }

    public List<Map<String, Object>> searchCommodity(List<Float> embedding, Integer topK) {
        return search(milvusProperties.getCommodityCollection(), normalizeDimension(embedding), topK);
    }

    public List<Map<String, Object>> searchUserProfile(List<Float> embedding, Integer topK) {
        return search(milvusProperties.getUserProfileCollection(), normalizeDimension(embedding), topK);
    }

    public List<Map<String, Object>> searchCommodityByText(String queryText, Integer topK) {
        return searchCommodity(vectorEmbeddingService.embedText(queryText), topK);
    }

    public List<Map<String, Object>> searchUserProfileByText(String queryText, Integer topK) {
        return searchUserProfile(vectorEmbeddingService.embedText(queryText), topK);
    }

    public void upsertConversationMessageByText(String messageId, String conversationId, String content) {
        upsert(milvusProperties.getConversationCollection(), messageId, conversationId, content,
                normalizeDimension(vectorEmbeddingService.embedText(content)));
    }

    public List<Map<String, Object>> searchConversationByText(String queryText, String conversationId, Integer topK) {
        List<Map<String, Object>> hits = search(
                milvusProperties.getConversationCollection(),
                normalizeDimension(vectorEmbeddingService.embedText(queryText)),
                topK
        );
        if (conversationId == null || conversationId.isBlank()) {
            return hits;
        }
        return hits.stream().filter(hit -> {
            Object entityRaw = hit.get("entity");
            if (!(entityRaw instanceof Map<?, ?> entity)) {
                return false;
            }
            Object bizId = entity.get("bizId");
            return bizId != null && conversationId.equals(String.valueOf(bizId));
        }).toList();
    }

    private void upsert(String collectionName, String id, String bizId, String content, List<Float> embedding) {
        validateInput(id, embedding);
        JsonObject row = new JsonObject();
        row.addProperty(ID_FIELD, id);
        row.addProperty(BIZ_ID_FIELD, StringUtils.hasText(bizId) ? bizId : id);
        row.addProperty(CONTENT_FIELD, content == null ? "" : content);
        JsonArray vector = new JsonArray();
        for (Float value : embedding) {
            vector.add(value);
        }
        row.add(VECTOR_FIELD, vector);
        milvusClient.upsert(UpsertReq.builder()
                .collectionName(collectionName)
                .data(List.of(row))
                .build());
    }

    private List<Map<String, Object>> search(String collectionName, List<Float> embedding, Integer topK) {
        if (embedding == null || embedding.isEmpty()) {
            return List.of();
        }
        int k = topK == null || topK <= 0 ? milvusProperties.getTopK() : topK;
        SearchReq req = SearchReq.builder()
                .collectionName(collectionName)
                .annsField(VECTOR_FIELD)
                .topK(k)
                .outputFields(List.of(ID_FIELD, BIZ_ID_FIELD, CONTENT_FIELD))
                .data(List.of(new FloatVec(toFloatArray(embedding))))
                .build();
        SearchResp resp = milvusClient.search(req);
        List<Map<String, Object>> result = new ArrayList<>();
        if (resp == null || resp.getSearchResults() == null) {
            return result;
        }
        for (List<SearchResp.SearchResult> group : resp.getSearchResults()) {
            for (SearchResp.SearchResult hit : group) {
                result.add(Map.of(
                        "id", hit.getId(),
                        "score", hit.getScore(),
                        "entity", hit.getEntity() == null ? Map.of() : hit.getEntity()
                ));
            }
        }
        return result;
    }

    private void ensureCollection(String collectionName) {
        Boolean exists = milvusClient.hasCollection(HasCollectionReq.builder()
                .collectionName(collectionName)
                .build());
        if (Boolean.TRUE.equals(exists)) {
            milvusClient.loadCollection(LoadCollectionReq.builder().collectionName(collectionName).build());
            return;
        }

        CreateCollectionReq.CollectionSchema schema = milvusClient.createSchema()
                .addField(AddFieldReq.builder()
                        .fieldName(ID_FIELD)
                        .dataType(DataType.VarChar)
                        .isPrimaryKey(true)
                        .autoID(false)
                        .maxLength(128)
                        .build())
                .addField(AddFieldReq.builder()
                        .fieldName(BIZ_ID_FIELD)
                        .dataType(DataType.VarChar)
                        .maxLength(128)
                        .build())
                .addField(AddFieldReq.builder()
                        .fieldName(CONTENT_FIELD)
                        .dataType(DataType.VarChar)
                        .maxLength(4096)
                        .build())
                .addField(AddFieldReq.builder()
                        .fieldName(VECTOR_FIELD)
                        .dataType(DataType.FloatVector)
                        .dimension(milvusProperties.getDimension())
                        .build());

        IndexParam indexParam = IndexParam.builder()
                .fieldName(VECTOR_FIELD)
                .indexName(collectionName + "_embedding_idx")
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .metricType(IndexParam.MetricType.valueOf(milvusProperties.getMetricType().toUpperCase()))
                .build();

        milvusClient.createCollection(CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(List.of(indexParam))
                .build());
        milvusClient.loadCollection(LoadCollectionReq.builder().collectionName(collectionName).build());
    }

    private void validateInput(String id, List<Float> embedding) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("id 不能为空");
        }
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalArgumentException("embedding 不能为空");
        }
    }

    private float[] toFloatArray(List<Float> vector) {
        float[] arr = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            arr[i] = vector.get(i);
        }
        return arr;
    }

    private List<Float> normalizeDimension(List<Float> embedding) {
        if (embedding == null) {
            throw new IllegalArgumentException("embedding 不能为空");
        }
        int target = milvusProperties.getDimension();
        if (embedding.size() == target) {
            return embedding;
        }
        List<Float> adjusted = new ArrayList<>(target);
        int copy = Math.min(embedding.size(), target);
        for (int i = 0; i < copy; i++) {
            adjusted.add(embedding.get(i));
        }
        for (int i = copy; i < target; i++) {
            adjusted.add(0f);
        }
        return adjusted;
    }
}

