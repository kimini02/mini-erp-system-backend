package com.minierp.backend.domain.dashboard.controller;

import com.minierp.backend.domain.dashboard.dto.AdminDashboardResponseDto;
import com.minierp.backend.domain.dashboard.dto.DashboardProjectDto;
import com.minierp.backend.domain.dashboard.dto.DashboardResponseDto;
import com.minierp.backend.domain.dashboard.service.DashboardService;
import com.minierp.backend.global.response.ApiResponse;
import com.minierp.backend.global.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 대시보드 REST 컨트롤러
 * - 비즈니스 로직 없이 Service에 위임, 역할별 접근 제어는 Service에서 처리
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> getDashboardProgress(Authentication authentication) {
        DashboardResponseDto response = dashboardService.getDashboardStats(
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 진행률 조회가 완료되었습니다."));
    }

    @GetMapping("/admin-summary")
    public ResponseEntity<ApiResponse<AdminDashboardResponseDto>> getAdminSummary(Authentication authentication) {
        AdminDashboardResponseDto response = dashboardService.getAdminSummary(
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "관리자 대시보드 통계 조회가 완료되었습니다."));
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<DashboardProjectDto>>> getDashboardProjects(Authentication authentication) {
        List<DashboardProjectDto> response = dashboardService.getDashboardProjects(
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 프로젝트 현황 조회가 완료되었습니다."));
    }
}
