package com.minierp.backend.domain.dashboard.dto;

import com.minierp.backend.domain.approval.dto.LeaveRequestResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminDashboardResponseDto {

    private long totalUsers;
    private long activeProjectCount;
    private long pendingApprovalCount;
    private double taskCompletionRate;
    private long totalTaskCount;
    private List<LeaveRequestResponseDto> pendingApprovals;

    public static AdminDashboardResponseDto of(
            long totalUsers,
            long activeProjectCount,
            long pendingApprovalCount,
            double taskCompletionRate,
            long totalTaskCount,
            List<LeaveRequestResponseDto> pendingApprovals
    ) {
        return new AdminDashboardResponseDto(
                totalUsers,
                activeProjectCount,
                pendingApprovalCount,
                taskCompletionRate,
                totalTaskCount,
                pendingApprovals
        );
    }
}