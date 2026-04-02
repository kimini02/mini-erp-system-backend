package com.minierp.backend.domain.approval.dto;

import com.minierp.backend.domain.approval.entity.LeaveType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class LeaveRequestCreateDto {
    private LeaveType appType;
    private LocalDate startDate;
    private LocalDate endDate;
    // 신청자가 연차 사유를 작성할 수 있는 선택 입력값
    private String requestReason;
}
