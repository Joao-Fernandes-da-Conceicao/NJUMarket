package com.njumarket.commodity.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 搜索相关配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "commodity.search")
public class CommoditySearchProperties {

    /**
     * 是否开启搜索能力
     */
    private boolean enabled = true;

    private final Elasticsearch elasticsearch = new Elasticsearch();
    private final Sync sync = new Sync();

    @Data
    public static class Elasticsearch {
        /**
         * 是否启用 ElasticSearch
         */
        private boolean enabled = true;

        /**
         * 索引名称
         */
        private String index = "commodities";

        /**
         * 是否开启高亮
         */
        private boolean highlightEnabled = true;
    }

    @Data
    public static class Sync {
        /**
         * 应用启动时是否自动重建索引
         */
        private boolean autoReindexOnStartup = false;

        /**
         * 重建索引批大小
         */
        private int pageSize = 500;
    }
}

