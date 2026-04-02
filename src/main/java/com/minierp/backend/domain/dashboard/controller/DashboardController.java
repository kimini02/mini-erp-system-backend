package com.minierp.backend.domain.dashboard.controller;

import com.minierp.backend.domain.dashboard.dto.AdminDashboardResponseDto;
import com.minierp.backend.domain.dashboard.dto.DashboardProjectDto;
import com.minierp.backend.domain.dashboard.dto.DashboardResponseDto;
import com.minierp.backend.domain.dashboard.service.DashboardService;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import com.minierp.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> getDashboardProgress(Authentication authentication) {
        DashboardResponseDto response = dashboardService.getDashboardStats(
                extractUserId(authentication),
                extractUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 진행률 조회가 완료되었습니다."));
    }

    @GetMapping("/admin-summary")
    public ResponseEntity<ApiResponse<AdminDashboardResponseDto>> getAdminSummary(Authentication authentication) {
        AdminDashboardResponseDto response = dashboardService.getAdminSummary(
                extractUserId(authentication),
                extractUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "관리자 대시보드 통계 조회가 완료되었습니다."));
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<DashboardProjectDto>>> getDashboardProjects(Authentication authentication) {
        List<DashboardProjectDto> response = dashboardService.getDashboardProjects(
                extractUserId(authentication),
                extractUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 프로젝트 현황 조회가 완료되었습니다."));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증 사용자 정보가 올바르지 않습니다.");
        }
    }

    private UserRole extractUserRole(Authentication authentication) {
        if (authentication == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.replace("ROLE_", ""))
                .map(UserRole::valueOf)
                .findFirst()
                .orElse(UserRole.USER);
    }
}
