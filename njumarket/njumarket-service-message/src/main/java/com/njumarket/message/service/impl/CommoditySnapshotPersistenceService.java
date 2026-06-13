package com.njumarket.message.service.impl;

import com.njumarket.message.entity.CommoditySnapshot;
import com.njumarket.message.repository.CommoditySnapshotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 商品快照持久化服务。
 *
 * 使用独立事务（REQUIRES_NEW）保存快照，确保快照保存失败时仅回滚本次子事务，
 * 不污染调用方（sendMessage）的主事务，使消息本体仍能正常入库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommoditySnapshotPersistenceService {

    private final CommoditySnapshotRepository commoditySnapshotRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommoditySnapshot saveSnapshot(String messageId, String commodityId,
                                          Map<String, Object> commodityData,
                                          String firstImageUrl) {
        CommoditySnapshot snapshot = new CommoditySnapshot();
        snapshot.setMessageId(messageId);
        snapshot.setCommodityId(commodityId);
        snapshot.setTitle((String) commodityData.get("title"));

        Object priceObj = commodityData.get("price");
        if (priceObj instanceof Number) {
            snapshot.setPrice(((Number) priceObj).doubleValue());
        }
        snapshot.setImageUrl(firstImageUrl);
        snapshot.setStatus((String) commodityData.get("commodityStatus"));

        CommoditySnapshot saved = commoditySnapshotRepository.save(snapshot);
        log.debug("商品快照已保存: messageId={}, commodityId={}", messageId, commodityId);
        return saved;
    }
}
