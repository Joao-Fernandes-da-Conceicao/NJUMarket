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

    // 多对一关系：审核记录属于某个商品
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commodity_id", insertable = false, updatable = false)
    private Commodity commodity;

    /**
     * 创建审核记录
     * @return 创建是否成功
     */
    public Boolean createRecord() {
        this.auditTime = LocalDateTime.now();
        return true;
    }
}

