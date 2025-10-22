package com.njumarket.njumarket.controller.admin;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-投诉管理控制器
 */
@RestController
@RequestMapping("/api/admin/complaint")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final AdminService adminService;

    /**
     * 获取投诉列表
     */
    @GetMapping("/list")
    public Result getComplaints(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               @RequestParam(required = false) String status) {
        return adminService.getComplaints(page, size, status);
    }

    /**
     * 处理投诉
     */
    @PostMapping("/{complaintId}/handle")
    public Result handleComplaint(@PathVariable String complaintId, 
                                @RequestParam String decision,
                                @RequestParam(required = false) String remark) {
        return adminService.handleComplaint(complaintId, decision, remark);
    }

    /**
     * 获取投诉详情
     */
    @GetMapping("/{complaintId}")
    public Result getComplaintDetail(@PathVariable String complaintId) {
        return adminService.getComplaintDetail(complaintId);
    }

    /**
     * 批量处理投诉
     */
    @PostMapping("/batch-handle")
    public Result batchHandleComplaints(@RequestBody String[] complaintIds,
                                      @RequestParam String decision) {
        return adminService.batchHandleComplaints(complaintIds, decision);
    }

    /**
     * 获取投诉统计
     */
    @GetMapping("/statistics")
    public Result getComplaintStatistics() {
        return adminService.getComplaintStatistics();
    }
}
