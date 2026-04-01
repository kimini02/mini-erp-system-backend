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
     */
    @PostMapping
    public ResponseEntity<OvertimeResponseDto> requestOvertime(
            @RequestBody OvertimeRequestDto dto,
            @RequestHeader("X-User-Id") Long userId) {
        
        OvertimeResponseDto response = overtimeService.requestOvertime(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특근 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<OvertimeResponseDto> getOvertimeRequest(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long accessorId) {
        return ResponseEntity.ok(overtimeService.getOvertimeRequest(id, accessorId));
    }

    /**
     * 특근 승인
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
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectOvertime(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        
        overtimeService.rejectOvertime(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특근 내역 조회 (권한별 필터링)
     */
    @GetMapping("/list")
    public ResponseEntity<List<OvertimeResponseDto>> getOvertimeRequests(
            @RequestHeader("X-User-Id") Long userId) {
        
        List<OvertimeResponseDto> response = overtimeService.getOvertimeRequests(userId);
        return ResponseEntity.ok(response);
    }

    @Deprecated
    @GetMapping("/my")
    public ResponseEntity<List<OvertimeResponseDto>> getMyOvertimeRequests(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(overtimeService.getOvertimeRequests(userId));
    }
}
