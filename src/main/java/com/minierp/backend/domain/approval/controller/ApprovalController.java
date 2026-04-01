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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leave")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * 연차 신청 생성
     */
    @PostMapping
    public ResponseEntity<LeaveRequestResponseDto> createLeaveRequest(
            @RequestBody LeaveRequestCreateDto dto,
            @RequestHeader("X-User-Id") Long userId) {
        
        LeaveRequestResponseDto response = approvalService.createLeaveRequest(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 연차 단건 조회
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<LeaveRequestResponseDto> getLeaveRequest(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long accessorId) {
        return ResponseEntity.ok(approvalService.getLeaveRequest(requestId, accessorId));
    }

    /**
     * 직급별 연차 기준 조회
     */
    @GetMapping("/policy")
    public ResponseEntity<Map<String, Integer>> getLeavePolicy() {
        return ResponseEntity.ok(approvalService.getLeavePolicy());
    }

    /**
     * 연차 승인
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
     * 연차 내역 조회 (권한별 필터링)
     * GET /api/v1/leave/list
     */
    @GetMapping("/list")
    public ResponseEntity<List<LeaveRequestResponseDto>> getLeaveRequests(
            @RequestHeader("X-User-Id") Long userId) {
        
        List<LeaveRequestResponseDto> response = approvalService.getLeaveRequests(userId);
        return ResponseEntity.ok(response);
    }
    
    // 이전 /my, /all 통합 처리 (/list로 대체 가능하지만 하위 호환을 위해 유지 가능)
    @Deprecated
    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestResponseDto>> getMyLeaveRequests(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(approvalService.getLeaveRequests(userId));
    }
}
