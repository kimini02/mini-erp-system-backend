package com.minierp.backend.domain.dashboard.controller;

import com.minierp.backend.domain.dashboard.dto.DashboardResponseDto;
import com.minierp.backend.domain.dashboard.service.DashboardService;
import com.minierp.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> getDashboardProgress() {
        DashboardResponseDto response = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 진행률 조회가 완료되었습니다."));
    }
}
