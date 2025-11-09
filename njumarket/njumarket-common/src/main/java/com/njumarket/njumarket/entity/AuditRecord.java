package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 审核记录实体类
 * 记录商品审核的详细信息
 */
@Entity
@Table(name = "audit_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecord {

    @Id
    @Column(name = "record_id", length = 50)
    private String recordId;

    @Column(name = "commodity_id", length = 50, nullable = false)
    private String commodityId;

    @Column(name = "reviewer_id", length = 50)
    private String reviewerId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "decision", length = 20, nullable = false)
    private String decision; // APPROVED, REJECTED, PENDING

    @CreationTimestamp
    @Column(name = "audit_time", nullable = false)
    private LocalDateTime auditTime;

    @Column(name = "audit_type", length = 20, nullable = false)
    private String auditType; // AUTO, MANUAL

    // ⚠️ 注意：不再使用 @ManyToOne 关系，因为 Commodity 实体已迁移到 commodity-service
    // 如果需要访问商品信息，应通过 Feign Client 调用 commodity-service

    /**
     * 创建审核记录
     * @return 创建是否成功
     */
    public Boolean createRecord() {
        this.auditTime = LocalDateTime.now();
        return true;
    }
}

