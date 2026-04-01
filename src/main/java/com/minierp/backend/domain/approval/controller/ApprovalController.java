package com.minierp.backend.domain.approval.controller;

import com.minierp.backend.domain.approval.dto.LeaveBalanceResponseDto;
import com.minierp.backend.domain.approval.dto.LeavePolicyResponseDto;
import com.minierp.backend.domain.approval.dto.LeaveRequestCreateDto;
import com.minierp.backend.domain.approval.dto.LeaveRequestResponseDto;
import com.minierp.backend.domain.approval.dto.RejectRequestDto;
import com.minierp.backend.domain.approval.service.ApprovalService;
import com.minierp.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leave")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * 연차 신청 생성
     * POST /api/v1/leave
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LeaveRequestResponseDto>> createLeaveRequest(
            @RequestBody LeaveRequestCreateDto dto,
            Authentication authentication) {

        LeaveRequestResponseDto response = approvalService.createLeaveRequest(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "연차 신청이 완료되었습니다."));
    }

    /**
     * 연차 승인
     * PATCH /api/v1/leave/{requestId}/approve
     */
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDto>> approveLeaveRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        LeaveRequestResponseDto response = approvalService.approveLeaveRequest(requestId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "연차가 승인되었습니다."));
    }

    /**
     * 연차 반려
     * PATCH /api/v1/leave/{requestId}/reject
     */
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDto>> rejectLeaveRequest(
            @PathVariable Long requestId,
            @RequestBody RejectRequestDto rejectDto,
            Authentication authentication) {

        LeaveRequestResponseDto response = approvalService.rejectLeaveRequest(requestId, authentication.getName(), rejectDto);
        return ResponseEntity.ok(ApiResponse.success(response, "연차가 반려되었습니다."));
    }

    /**
     * 내 신청 내역 조회
     * GET /api/v1/leave/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponseDto>>> getMyLeaveRequests(
            Authentication authentication) {

        List<LeaveRequestResponseDto> response = approvalService.getMyLeaveRequests(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "내 연차 신청 내역 조회가 완료되었습니다."));
    }

    /**
     * 전체 결재 내역 조회 (관리자용)
     * GET /api/v1/leave/all
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponseDto>>> getAllLeaveRequests(Authentication authentication) {
        List<LeaveRequestResponseDto> response = approvalService.getAllLeaveRequests(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "연차 신청 전체 조회가 완료되었습니다."));
    }

    /**
     * 연차 잔여 현황 조회
     * GET /api/v1/leave/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<LeaveBalanceResponseDto>> getLeaveBalance(
            Authentication authentication) {

        LeaveBalanceResponseDto response = approvalService.getLeaveBalance(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "연차 잔여 현황 조회가 완료되었습니다."));
    }

    /**
     * 연차 정책 조회
     * GET /api/v1/leave/policy
     */
    @GetMapping("/policy")
    public ResponseEntity<ApiResponse<List<LeavePolicyResponseDto>>> getLeavePolicy() {
        List<LeavePolicyResponseDto> response = approvalService.getLeavePolicy();
        return ResponseEntity.ok(ApiResponse.success(response, "연차 정책 조회가 완료되었습니다."));
    }
}
