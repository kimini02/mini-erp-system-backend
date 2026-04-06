package com.minierp.backend.domain.overtime.controller;

import com.minierp.backend.domain.overtime.dto.OvertimeRequestDto;
import com.minierp.backend.domain.overtime.dto.OvertimeResponseDto;
import com.minierp.backend.domain.overtime.service.OvertimeService;
import com.minierp.backend.global.response.ApiResponse;
import com.minierp.backend.global.security.CurrentUserResolver;
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
    private final CurrentUserResolver currentUserResolver;

    /**
     * 특근 신청
     * POST /api/v1/overtime
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OvertimeResponseDto>> requestOvertime(
            @RequestBody OvertimeRequestDto dto,
            Authentication authentication) {

        OvertimeResponseDto response = overtimeService.requestOvertime(dto, currentUserResolver.resolveUserId(authentication));
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

        OvertimeResponseDto response = overtimeService.approveOvertime(id, currentUserResolver.resolveUserId(authentication));
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

        OvertimeResponseDto response = overtimeService.rejectOvertime(id, currentUserResolver.resolveUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "특근이 반려되었습니다."));
    }

    /**
     * 특근 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OvertimeResponseDto>> getOvertimeRequest(
            @PathVariable Long id,
            Authentication authentication) {

        OvertimeResponseDto response = overtimeService.getOvertimeRequest(id, currentUserResolver.resolveUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "특근 단건 조회가 완료되었습니다."));
    }

    /**
     * 특근 내역 조회 (권한별 필터링)
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<OvertimeResponseDto>>> getOvertimeRequests(
            Authentication authentication) {

        List<OvertimeResponseDto> response = overtimeService.getOvertimeRequestList(currentUserResolver.resolveUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "특근 내역 조회가 완료되었습니다."));
    }

    /**
     * 🚩 [추가] 관리자/팀장용 전체 특근 내역 조회
     * GET /api/v1/overtime/all
     * 프론트엔드 대시보드에서 호출하는 엔드포인트입니다.
     * ADMIN/TEAM_LEADER만 접근 가능
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<OvertimeResponseDto>>> getAllOvertimeRequests(
            Authentication authentication) {

        Long userId = currentUserResolver.resolveUserId(authentication);
        // ADMIN/TEAM_LEADER만 접근 가능하도록 필터링
        List<OvertimeResponseDto> response = overtimeService.getAllOvertimeRequestList(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "전체 특근 내역 조회가 완료되었습니다."));
    }

    @Deprecated
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OvertimeResponseDto>>> getMyOvertimeRequests(Authentication authentication) {
        List<OvertimeResponseDto> response = overtimeService.getOvertimeRequestList(currentUserResolver.resolveUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "내 특근 신청 내역 조회가 완료되었습니다."));
    }
}
