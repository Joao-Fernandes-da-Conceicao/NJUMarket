package com.njumarket.message.repository;

import com.njumarket.message.entity.CommoditySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommoditySnapshotRepository extends JpaRepository<CommoditySnapshot, String> {

    Optional<CommoditySnapshot> findByMessageId(String messageId);

    List<CommoditySnapshot> findByMessageIdIn(List<String> messageIds);
}
