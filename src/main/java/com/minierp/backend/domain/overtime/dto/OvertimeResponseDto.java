package com.minierp.backend.domain.overtime.dto;

import com.minierp.backend.domain.overtime.entity.OvertimeRequest;
import com.minierp.backend.domain.overtime.entity.OvertimeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeResponseDto {
    private Long id;
    private Long requesterId;
    private String requesterName;
    private String departmentCode;
    private String userRole;
    private Long approverId;
    private String approverName;
    private LocalDate overtimeDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private OvertimeStatus status;
    private LocalDateTime createdAt;

    public static OvertimeResponseDto from(OvertimeRequest request) {
        return new OvertimeResponseDto(
                request.getId(),
                request.getRequester().getId(),
                request.getRequester().getUserName(),
                request.getRequester().getDepartmentCode(),
                request.getRequester().getUserRole().name(),
                request.getApprover() != null ? request.getApprover().getId() : null,
                request.getApprover() != null ? request.getApprover().getUserName() : null,
                request.getOvertimeDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getReason(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
