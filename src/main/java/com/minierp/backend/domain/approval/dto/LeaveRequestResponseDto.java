package com.minierp.backend.domain.approval.dto;

import com.minierp.backend.domain.approval.entity.LeaveRequest;
import com.minierp.backend.domain.approval.entity.LeaveStatus;
import com.minierp.backend.domain.approval.entity.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponseDto {
    private Long appId;
    private Long requesterId;
    private String requesterName;
    private String departmentCode;
    private String userRole;
    private Long approverId;
    private String approverName;
    private LeaveType appType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal usedDays;
    private LeaveStatus appStatus;
    private String rejectReason;
    private String requestReason;
    private LocalDateTime createdAt;

    public static LeaveRequestResponseDto from(LeaveRequest leaveRequest) {
        return new LeaveRequestResponseDto(
                leaveRequest.getId(),
                leaveRequest.getRequester().getId(),
                leaveRequest.getRequester().getUserName(),
                leaveRequest.getRequester().getDepartmentCode(),
                leaveRequest.getRequester().getUserRole().name(),
                leaveRequest.getApprover() != null ? leaveRequest.getApprover().getId() : null,
                leaveRequest.getApprover() != null ? leaveRequest.getApprover().getUserName() : null,
                leaveRequest.getAppType(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getUsedDays(),
                leaveRequest.getAppStatus(),
                leaveRequest.getRejectReason(),
                leaveRequest.getRequestReason(),
                leaveRequest.getCreatedAt()
        );
    }
}
