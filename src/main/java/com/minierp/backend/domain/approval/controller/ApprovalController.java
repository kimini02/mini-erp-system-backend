package com.minierp.backend.domain.approval.controller;

import com.minierp.backend.domain.approval.dto.LeaveRequestCreateDto;
import com.minierp.backend.domain.approval.dto.LeaveRequestResponseDto;
import com.minierp.backend.domain.approval.dto.RejectRequestDto;
import com.minierp.backend.domain.approval.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<LeaveRequestResponseDto> createLeaveRequest(
            @RequestBody LeaveRequestCreateDto dto,
            @RequestHeader("X-User-Id") Long userId) { // 현재는 단순 헤더로 ID 전달 (추후 시큐리티 적용 가능)
        
        LeaveRequestResponseDto response = approvalService.createLeaveRequest(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 연차 승인
     * PATCH /api/v1/leave/{requestId}/approve
     */
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<Void> approveLeaveRequest(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long userId) {
        
        approvalService.approveLeaveRequest(requestId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 연차 반려
     * PATCH /api/v1/leave/{requestId}/reject
     */
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<Void> rejectLeaveRequest(
            @PathVariable Long requestId,
            @RequestBody RejectRequestDto rejectDto,
            @RequestHeader("X-User-Id") Long userId) {
        
        approvalService.rejectLeaveRequest(requestId, userId, rejectDto);
        return ResponseEntity.ok().build();
    }

    /**
     * 내 신청 내역 조회
     * GET /api/v1/leave/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestResponseDto>> getMyLeaveRequests(
            @RequestHeader("X-User-Id") Long userId) {
        
        List<LeaveRequestResponseDto> response = approvalService.getMyLeaveRequests(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 결재 내역 조회 (관리자용)
     * GET /api/v1/leave/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<LeaveRequestResponseDto>> getAllLeaveRequests() {
        List<LeaveRequestResponseDto> response = approvalService.getAllLeaveRequests();
        return ResponseEntity.ok(response);
    }
}
