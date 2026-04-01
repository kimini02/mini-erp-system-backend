package com.minierp.backend.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminDashboardResponseDto {

    private long totalUsers;
    private long activeProjectCount;
    private long pendingApprovalCount;
    private double taskCompletionRate;
    private long totalTaskCount;

    public static AdminDashboardResponseDto of(
            long totalUsers,
            long activeProjectCount,
            long pendingApprovalCount,
            double taskCompletionRate,
            long totalTaskCount
    ) {
        return new AdminDashboardResponseDto(
                totalUsers,
                activeProjectCount,
                pendingApprovalCount,
                taskCompletionRate,
                totalTaskCount
        );
    }
}
