package com.njumarket.njumarket.repository;

import com.njumarket.njumarket.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 投诉数据访问层
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, String> {
    
    /**
     * 根据投诉人ID查找投诉
     */
    List<Complaint> findByComplainantId(String complainantId);
    
    /**
     * 根据被投诉人ID查找投诉
     */
    List<Complaint> findByDefendantId(String defendantId);
    
    /**
     * 根据状态查找投诉
     */
    List<Complaint> findByStatus(String status);
    
    /**
     * 根据相关订单ID查找投诉
     */
    List<Complaint> findByRelatedOrderId(String relatedOrderId);
    
    /**
     * 获取待处理投诉
     */
    @Query("SELECT c FROM Complaint c WHERE c.status IN ('SUBMITTED', 'PROCESSING') ORDER BY c.submitTime ASC")
    List<Complaint> findPendingComplaints();
}
