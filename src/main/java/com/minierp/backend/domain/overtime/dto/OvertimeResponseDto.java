package com.minierp.backend.domain.overtime.dto;

import com.minierp.backend.domain.overtime.entity.OvertimeRequest;
import com.minierp.backend.domain.overtime.entity.OvertimeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeResponseDto {
    private Long id;
    private Long requesterId;
    private String requesterName;
    private Long approverId;
    private String approverName;
    private LocalDate overtimeDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private OvertimeStatus status;
    private LocalDateTime createdAt;

    public static OvertimeResponseDto from(OvertimeRequest request) {
        return OvertimeResponseDto.builder()
                .id(request.getId())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getUserName())
                .approverId(request.getApprover() != null ? request.getApprover().getId() : null)
                .approverName(request.getApprover() != null ? request.getApprover().getUserName() : null)
                .overtimeDate(request.getOvertimeDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
