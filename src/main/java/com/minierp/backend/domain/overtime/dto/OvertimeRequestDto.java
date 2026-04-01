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
    private LocalDate overtimeDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
}
