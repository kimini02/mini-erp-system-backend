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
}
