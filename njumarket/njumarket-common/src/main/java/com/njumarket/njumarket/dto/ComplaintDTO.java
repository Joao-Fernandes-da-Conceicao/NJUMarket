package com.njumarket.njumarket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 投诉数据传输对象
 */
@Data
public class ComplaintDTO {
    private String complaintId;
    
    private String complainantId;
    
    @NotBlank(message = "被投诉人ID不能为空")
    private String defendantId;
    
    @NotBlank(message = "相关订单ID不能为空")
    private String relatedOrderId;
    
    @NotBlank(message = "投诉内容不能为空")
    private String content;
    
    private List<String> evidenceFiles;
    private String status;
    
    @NotBlank(message = "投诉类型不能为空")
    private String complaintType; // ORDER_ISSUE, QUALITY_PROBLEM, FRAUD, OTHER
}

