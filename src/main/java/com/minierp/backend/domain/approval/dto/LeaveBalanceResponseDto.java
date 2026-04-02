package com.minierp.backend.domain.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class LeaveBalanceResponseDto {
    private BigDecimal totalAnnualLeave;
    private BigDecimal usedAnnualLeave;
    private BigDecimal remainingAnnualLeave;
}

