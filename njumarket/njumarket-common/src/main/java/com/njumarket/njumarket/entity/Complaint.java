package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投诉实体类
 * 处理用户之间的投诉和纠纷
 */
@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {
    
    @Id
    @Column(name = "complaint_id", length = 50)
    private String complaintId;
    
    @Column(name = "complainant_id", length = 50, nullable = false)
    private String complainantId;
    
    @Column(name = "defendant_id", length = 50, nullable = false)
    private String defendantId;
    
    @Column(name = "related_order_id", length = 50)
    private String relatedOrderId;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "evidence_files", columnDefinition = "TEXT")
    private String evidenceFiles; // JSON格式存储文件列表
    
    @Column(name = "status", length = 20, nullable = false)
    private String status; // SUBMITTED, PROCESSING, RESOLVED, REJECTED
    
    @CreationTimestamp
    @Column(name = "submit_time", nullable = false)
    private LocalDateTime submitTime;
    
    @Column(name = "resolve_time")
    private LocalDateTime resolveTime;
    
    // 多对一关系：投诉属于某个投诉人
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complainant_id", insertable = false, updatable = false)
    private User complainant;
    
    // 多对一关系：投诉针对某个被投诉人
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defendant_id", insertable = false, updatable = false)
    private User defendant;
    
    // 多对一关系：投诉关联某个订单
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_order_id", insertable = false, updatable = false)
    private Order relatedOrder;
    
    /**
     * 提交投诉
     * @return 提交是否成功
     */
    public Boolean submitComplaint() {
        this.status = "SUBMITTED";
        this.submitTime = LocalDateTime.now();
        return true;
    }
    
    /**
     * 更新投诉处理进度
     * @param status 新状态
     * @return 更新是否成功
     */
    public Boolean updateProgress(String status) {
        this.status = status;
        if ("RESOLVED".equals(status) || "REJECTED".equals(status)) {
            this.resolveTime = LocalDateTime.now();
        }
        return true;
    }
}

