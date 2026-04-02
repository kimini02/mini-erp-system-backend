package com.minierp.backend.domain.overtime.controller;

import com.minierp.backend.domain.overtime.dto.OvertimeRequestDto;
import com.minierp.backend.domain.overtime.dto.OvertimeResponseDto;
import com.minierp.backend.domain.overtime.service.OvertimeService;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import com.minierp.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<ApiResponse<OvertimeResponseDto>> requestOvertime(
            @RequestBody OvertimeRequestDto dto,
            Authentication authentication) {

        OvertimeResponseDto response = overtimeService.requestOvertime(dto, extractUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "특근 신청이 완료되었습니다."));
    }

    /**
     * 특근 승인
     * PATCH /api/v1/overtime/{id}/approve
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<OvertimeResponseDto>> approveOvertime(
            @PathVariable Long id,
            Authentication authentication) {

        OvertimeResponseDto response = overtimeService.approveOvertime(id, extractUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "특근이 승인되었습니다."));
    }

    /**
     * 특근 반려
     * PATCH /api/v1/overtime/{id}/reject
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<OvertimeResponseDto>> rejectOvertime(
            @PathVariable Long id,
            Authentication authentication) {

        OvertimeResponseDto response = overtimeService.rejectOvertime(id, extractUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "특근이 반려되었습니다."));
    }

    /**
     * 특근 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OvertimeResponseDto>> getOvertimeRequest(
            @PathVariable Long id,
            Authentication authentication) {

        OvertimeResponseDto response = overtimeService.getOvertimeRequest(id, extractUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "특근 단건 조회가 완료되었습니다."));
    }

    /**
     * 특근 내역 조회 (권한별 필터링)
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<OvertimeResponseDto>>> getOvertimeRequests(
            Authentication authentication) {

        List<OvertimeResponseDto> response = overtimeService.getOvertimeRequests(extractUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "특근 내역 조회가 완료되었습니다."));
    }

    @Deprecated
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OvertimeResponseDto>>> getMyOvertimeRequests(Authentication authentication) {
        List<OvertimeResponseDto> response = overtimeService.getOvertimeRequests(extractUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "내 특근 신청 내역 조회가 완료되었습니다."));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증 사용자 정보가 올바르지 않습니다.");
        }
    }
}
