package com.minierp.backend.domain.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LeavePolicyResponseDto {
    private String position;
    private int annualLeaveDays;
}

