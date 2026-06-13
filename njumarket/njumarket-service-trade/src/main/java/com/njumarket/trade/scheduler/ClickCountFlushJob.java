package com.njumarket.trade.scheduler;

import com.njumarket.njumarket.utils.RedisConstants;
import com.njumarket.trade.repository.CommodityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 点击量 Write-Behind 定时刷回任务
 *
 * 核心思路：
 *   每次用户访问商品时，只向 Redis INCR 一个计数器（click:count:delta:{commodityId}），
 *   不直接操作数据库，彻底消除高并发写热点。
 *   本 Job 每分钟扫描所有增量 key，将累积值一次性以 SQL UPDATE 原子加回数据库，
 *   然后删除该 Redis key。
 *
 * 注意：
 *   - 使用 SCAN 代替 KEYS，避免在大 keyspace 下阻塞 Redis。
 *   - GETDEL 原子地取出并清除 key，防止并发刷任务重复计数。
 *   - 每条 UPDATE 使用 click_count = click_count + :delta，无需先 SELECT，
 *     避免丢失并发写入的增量。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickCountFlushJob {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CommodityRepository commodityRepository;

    /**
     * 每 60 秒执行一次：将 Redis 中所有点击量增量批量刷回数据库
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void flushClickCounts() {
        log.debug("开始执行点击量刷回任务");

        // 1. 用 SCAN 游标遍历所有增量 key（避免 KEYS 阻塞）
        Map<String, Integer> deltaMap = new HashMap<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(RedisConstants.CLICK_COUNT_DELTA_PATTERN)
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();

                // 2. GETDEL：原子取出并清除，避免重复计算同一批次增量
                Object raw = redisTemplate.opsForValue().getAndDelete(key);
                if (raw == null) {
                    continue;
                }

                int delta;
                try {
                    delta = ((Number) raw).intValue();
                } catch (ClassCastException e) {
                    // INCR 存储的是整数，Jackson 反序列化后为 Integer/Long
                    delta = Integer.parseInt(raw.toString());
                }

                if (delta <= 0) {
                    continue;
                }

                String commodityId = key.substring(RedisConstants.CLICK_COUNT_DELTA_KEY.length());
                deltaMap.put(commodityId, delta);
            }
        } catch (Exception e) {
            log.error("扫描点击量增量 key 失败: {}", e.getMessage(), e);
            return;
        }

        if (deltaMap.isEmpty()) {
            log.debug("本次无点击量增量，跳过 DB 刷回");
            return;
        }

        // 3. 逐条原子 UPDATE（click_count = click_count + delta），无竞态条件
        int successCount = 0;
        for (Map.Entry<String, Integer> entry : deltaMap.entrySet()) {
            try {
                commodityRepository.incrementClickCount(entry.getKey(), entry.getValue());
                successCount++;
            } catch (Exception e) {
                log.error("点击量刷回失败 - commodityId={}, delta={}, error={}",
                        entry.getKey(), entry.getValue(), e.getMessage());
                // 单条失败不中断整批次；已 GETDEL 的增量丢失，但属于可接受的最终一致性损耗
            }
        }

        log.info("点击量刷回完成，成功 {}/{} 条", successCount, deltaMap.size());
    }
}
