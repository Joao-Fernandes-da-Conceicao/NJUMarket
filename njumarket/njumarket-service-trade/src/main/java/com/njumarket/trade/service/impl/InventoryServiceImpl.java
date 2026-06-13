package com.njumarket.trade.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.trade.repository.CommodityInventoryRepository;
import com.njumarket.trade.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final CommodityInventoryRepository inventoryRepository;

    @Override
    @Transactional
    public Result syncInventory(String commodityId, int availableQuantity, int totalQuantity) {
        if (commodityId == null || commodityId.isBlank()) {
            throw new BusinessException("commodityId 不能为空");
        }
        inventoryRepository.upsertStock(
                commodityId,
                Math.max(0, availableQuantity),
                Math.max(0, totalQuantity),
                LocalDateTime.now());
        log.info("库存同步成功 - commodityId={}, available={}, total={}", commodityId, availableQuantity, totalQuantity);
        return Result.ok("库存同步成功");
    }

    @Override
    @Transactional
    public Result adjustInventory(String commodityId, int newTotalQuantity) {
        if (commodityId == null || commodityId.isBlank()) {
            throw new BusinessException("commodityId 不能为空");
        }
        if (newTotalQuantity < 0) {
            throw new BusinessException("newTotalQuantity 不能为负");
        }

        var opt = inventoryRepository.findByCommodityId(commodityId);
        if (opt.isEmpty()) {
            // 记录不存在则按 sync 语义创建（如曾漏同步）
            inventoryRepository.upsertStock(commodityId, newTotalQuantity, newTotalQuantity, LocalDateTime.now());
            log.info("库存调整：记录不存在已创建 - commodityId={}, total={}", commodityId, newTotalQuantity);
            return Result.ok("库存已调整");
        }

        var inv = opt.get();
        int oldTotal = inv.getTotalQuantity();
        int delta = newTotalQuantity - oldTotal;
        int newAvailable = Math.max(0, Math.min(inv.getAvailableQuantity() + delta, newTotalQuantity));
        inv.setTotalQuantity(newTotalQuantity);
        inv.setAvailableQuantity(newAvailable);
        inv.setUpdatedAt(LocalDateTime.now());
        inventoryRepository.save(inv);

        log.info("库存调整成功 - commodityId={}, oldTotal={}, newTotal={}, newAvailable={}",
                commodityId, oldTotal, newTotalQuantity, newAvailable);
        return Result.ok("库存已调整");
    }

    @Override
    @Transactional
    public Result zeroInventory(String commodityId) {
        int rows = inventoryRepository.zeroStock(commodityId, LocalDateTime.now());
        if (rows == 0) {
            log.warn("归零库存：记录不存在 - commodityId={}", commodityId);
        }
        return Result.ok("库存已归零");
    }

    @Override
    public Result getInventory(String commodityId) {
        return inventoryRepository.findByCommodityId(commodityId)
                .map(inv -> Result.ok("查询成功", inv))
                .orElse(Result.fail("库存记录不存在"));
    }
}
