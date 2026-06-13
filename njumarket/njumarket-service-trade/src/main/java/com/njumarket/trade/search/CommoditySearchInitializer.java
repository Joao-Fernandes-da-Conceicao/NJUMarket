package com.njumarket.trade.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 应用启动时初始化搜索索引
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommoditySearchInitializer implements CommandLineRunner {

    private final CommoditySearchService commoditySearchService;
    private final CommoditySearchProperties properties;

    @Override
    public void run(String... args) {
        if (!commoditySearchService.isEnabled()) {
            log.info("ElasticSearch 搜索已禁用，跳过索引初始化");
            return;
        }
        if (!properties.getSync().isAutoReindexOnStartup()) {
            log.info("已跳过自动重建索引，可通过内部接口手动触发");
            return;
        }

        CompletableFuture.runAsync(() -> {
            long indexed = commoditySearchService.rebuildIndex();
            log.info("启动完成，自动重建 ElasticSearch 索引，数量={}", indexed);
        });
    }
}

