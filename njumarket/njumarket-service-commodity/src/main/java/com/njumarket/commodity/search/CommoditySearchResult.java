package com.njumarket.commodity.search;

import java.util.List;
import java.util.Map;

/**
 * 搜索结果封装
 */
public record CommoditySearchResult(
        List<String> commodityIds,
        long totalHits,
        Map<String, List<String>> highlights
) { }

