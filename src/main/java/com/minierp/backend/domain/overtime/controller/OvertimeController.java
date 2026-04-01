package com.minierp.backend.domain.overtime.controller;

import com.minierp.backend.domain.overtime.dto.OvertimeRequestDto;
import com.minierp.backend.domain.overtime.dto.OvertimeResponseDto;
import com.minierp.backend.domain.overtime.service.OvertimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    /**
     * 특근 신청
     * POST /api/v1/overtime
     */
    @PostMapping
    public ResponseEntity<OvertimeResponseDto> requestOvertime(
            @RequestBody OvertimeRequestDto dto,
            @RequestHeader("X-User-Id") Long userId) {
        
        OvertimeResponseDto response = overtimeService.requestOvertime(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특근 승인
     * PATCH /api/v1/overtime/{id}/approve
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approveOvertime(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        
        overtimeService.approveOvertime(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특근 반려
     * PATCH /api/v1/overtime/{id}/reject
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectOvertime(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        
        overtimeService.rejectOvertime(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 내 특근 신청 내역 조회
     * GET /api/v1/overtime/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<OvertimeResponseDto>> getMyOvertimeRequests(
            @RequestHeader("X-User-Id") Long userId) {
        
        List<OvertimeResponseDto> response = overtimeService.getMyOvertimeRequests(userId);
        return ResponseEntity.ok(response);
    }
}
