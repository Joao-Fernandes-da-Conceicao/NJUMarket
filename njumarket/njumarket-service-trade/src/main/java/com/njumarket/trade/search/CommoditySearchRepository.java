package com.njumarket.trade.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommoditySearchRepository extends ElasticsearchRepository<CommoditySearchDocument, String> {
}

