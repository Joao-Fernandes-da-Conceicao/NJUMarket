package com.njumarket.order.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.ComplaintDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 投诉服务接口
 */
public interface ComplaintService {
    
    /**
     * 提交投诉
     */
    Result submitComplaint(ComplaintDTO complaintDTO);
    
    /**
     * 上传投诉证据
     */
    Result uploadEvidence(MultipartFile[] files);
    
    /**
     * 获取我的投诉列表
     */
    Result getMyComplaints(Integer page, Integer size, String status);
    
    /**
     * 获取针对我的投诉列表
     */
    Result getComplaintsAgainstMe(Integer page, Integer size, String status);
    
    /**
     * 获取投诉详情
     */
    Result getComplaintDetail(String complaintId);
    
    /**
     * 撤销投诉
     */
    Result withdrawComplaint(String complaintId);
    
    /**
     * 回应投诉
     */
    Result respondToComplaint(String complaintId, String response);
    
    /**
     * 查看投诉处理进度
     */
    Result getComplaintProgress(String complaintId);
}

