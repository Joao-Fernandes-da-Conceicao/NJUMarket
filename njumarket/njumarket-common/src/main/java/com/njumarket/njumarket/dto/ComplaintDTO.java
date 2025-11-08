package com.njumarket.njumarket.dto;

import lombok.Data;

import java.util.List;

/**
 * 投诉数据传输对象
 */
@Data
public class ComplaintDTO {
    private String complaintId;
    private String complainantId;
    private String defendantId;
    private String relatedOrderId;
    private String content;
    private List<String> evidenceFiles;
    private String status;
    private String complaintType; // ORDER_ISSUE, QUALITY_PROBLEM, FRAUD, OTHER
}

