package com.njumarket.ai.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorEmbeddingService {

    private final EmbeddingModel embeddingModel;

    public List<Float> embedText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("text 不能为空");
        }
        Embedding embedding = embeddingModel.embed(text).content();
        float[] arr = embedding.vector();
        List<Float> vector = new ArrayList<>(arr.length);
        for (float v : arr) {
            vector.add(v);
        }
        return vector;
    }
}

