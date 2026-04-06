package com.minierp.backend.domain.overtime.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class OvertimeRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;

    // 기존 필드와의 호환성 유지 (필요 시)
    public LocalDate getOvertimeDate() {
        return startDate;
    }
}
