package com.njumarket.njumarket.controller.user;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.ComplaintDTO;
import com.njumarket.njumarket.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户投诉控制器
 */
@RestController
@RequestMapping("/api/user/complaint")
@RequiredArgsConstructor
public class UserComplaintController {

    private final ComplaintService complaintService;

    /**
     * 提交投诉
     */
    @PostMapping("/submit")
    public Result submitComplaint(@RequestBody ComplaintDTO complaintDTO) {
        return complaintService.submitComplaint(complaintDTO);
    }

    /**
     * 上传投诉证据
     */
    @PostMapping("/upload-evidence")
    public Result uploadEvidence(@RequestParam("files") MultipartFile[] files) {
        return complaintService.uploadEvidence(files);
    }

    /**
     * 获取我的投诉列表
     */
    @GetMapping("/my")
    public Result getMyComplaints(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size,
                                 @RequestParam(required = false) String status) {
        return complaintService.getMyComplaints(page, size, status);
    }

    /**
     * 获取针对我的投诉列表
     */
    @GetMapping("/against-me")
    public Result getComplaintsAgainstMe(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       @RequestParam(required = false) String status) {
        return complaintService.getComplaintsAgainstMe(page, size, status);
    }

    /**
     * 获取投诉详情
     */
    @GetMapping("/{complaintId}")
    public Result getComplaintDetail(@PathVariable String complaintId) {
        return complaintService.getComplaintDetail(complaintId);
    }

    /**
     * 撤销投诉
     */
    @PostMapping("/{complaintId}/withdraw")
    public Result withdrawComplaint(@PathVariable String complaintId) {
        return complaintService.withdrawComplaint(complaintId);
    }

    /**
     * 回应投诉（被投诉人）
     */
    @PostMapping("/{complaintId}/respond")
    public Result respondToComplaint(@PathVariable String complaintId,
                                   @RequestParam String response) {
        return complaintService.respondToComplaint(complaintId, response);
    }

    /**
     * 查看投诉处理进度
     */
    @GetMapping("/{complaintId}/progress")
    public Result getComplaintProgress(@PathVariable String complaintId) {
        return complaintService.getComplaintProgress(complaintId);
    }
}
