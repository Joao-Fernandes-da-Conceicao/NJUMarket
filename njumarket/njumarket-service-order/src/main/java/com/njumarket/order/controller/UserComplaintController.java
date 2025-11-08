package com.njumarket.order.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.ComplaintDTO;
import com.njumarket.order.service.ComplaintService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户投诉", description = "用户投诉相关功能")
@RestController
@RequestMapping("/api/user/complaint")
@RequiredArgsConstructor
public class UserComplaintController {

    private final ComplaintService complaintService;

    @Operation(summary = "提交投诉", description = "提交新的投诉")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "提交成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/submit")
    public Result submitComplaint(@Valid @RequestBody ComplaintDTO complaintDTO) {
        return complaintService.submitComplaint(complaintDTO);
    }

    @Operation(summary = "上传投诉证据", description = "上传投诉相关证据文件")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "文件格式错误")
    })
    @PostMapping(value = "/upload-evidence", consumes = "multipart/form-data")
    public Result uploadEvidence(@Parameter(description = "证据文件", required = true) @RequestParam("files") MultipartFile[] files) {
        return complaintService.uploadEvidence(files);
    }

    @Operation(summary = "获取我的投诉列表", description = "获取当前用户的投诉列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/my")
    public Result getMyComplaints(@Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
                                 @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
                                 @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return complaintService.getMyComplaints(page, size, status);
    }

    @Operation(summary = "获取针对我的投诉列表", description = "获取针对当前用户的投诉列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/against-me")
    public Result getComplaintsAgainstMe(@Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
                                       @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
                                       @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return complaintService.getComplaintsAgainstMe(page, size, status);
    }

    @Operation(summary = "获取投诉详情", description = "获取指定投诉的详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "投诉不存在")
    })
    @GetMapping("/{complaintId}")
    public Result getComplaintDetail(@Parameter(description = "投诉ID", required = true) @PathVariable String complaintId) {
        return complaintService.getComplaintDetail(complaintId);
    }

    @Operation(summary = "撤销投诉", description = "撤销已提交的投诉")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "撤销成功"),
        @ApiResponse(responseCode = "400", description = "投诉状态不允许撤销"),
        @ApiResponse(responseCode = "404", description = "投诉不存在")
    })
    @PostMapping("/{complaintId}/withdraw")
    public Result withdrawComplaint(@Parameter(description = "投诉ID", required = true) @PathVariable String complaintId) {
        return complaintService.withdrawComplaint(complaintId);
    }

    @Operation(summary = "回应投诉", description = "被投诉人回应投诉")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "回应成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "投诉不存在")
    })
    @PostMapping("/{complaintId}/respond")
    public Result respondToComplaint(@Parameter(description = "投诉ID", required = true) @PathVariable String complaintId,
                                   @Parameter(description = "回应内容", required = true) @RequestParam String response) {
        return complaintService.respondToComplaint(complaintId, response);
    }

    @Operation(summary = "查看投诉处理进度", description = "查看投诉的处理进度")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "投诉不存在")
    })
    @GetMapping("/{complaintId}/progress")
    public Result getComplaintProgress(@Parameter(description = "投诉ID", required = true) @PathVariable String complaintId) {
        return complaintService.getComplaintProgress(complaintId);
    }
}

