package com.minierp.backend.domain.approval.dto;

import com.minierp.backend.domain.approval.entity.LeaveRequest;
import com.minierp.backend.domain.approval.entity.LeaveStatus;
import com.minierp.backend.domain.approval.entity.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponseDto {
    private Long appId;
    private Long requesterId;
    private String requesterName;
    private Long approverId;
    private String approverName;
    private LeaveType appType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal usedDays;
    private LeaveStatus appStatus;
    private String rejectReason;
    private LocalDateTime createdAt;

    public static LeaveRequestResponseDto from(LeaveRequest leaveRequest) {
        return LeaveRequestResponseDto.builder()
                .appId(leaveRequest.getId())
                .requesterId(leaveRequest.getRequester().getId())
                .requesterName(leaveRequest.getRequester().getUserName())
                .approverId(leaveRequest.getApprover() != null ? leaveRequest.getApprover().getId() : null)
                .approverName(leaveRequest.getApprover() != null ? leaveRequest.getApprover().getUserName() : null)
                .appType(leaveRequest.getAppType())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .usedDays(leaveRequest.getUsedDays())
                .appStatus(leaveRequest.getAppStatus())
                .rejectReason(leaveRequest.getRejectReason())
                .createdAt(leaveRequest.getCreatedAt())
                .build();
    }
}
