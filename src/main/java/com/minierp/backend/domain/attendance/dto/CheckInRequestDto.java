package com.minierp.backend.domain.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class CheckInRequestDto {

    @NotNull(message = "근무일은 필수입니다.")
    private LocalDate workDate;

    @NotNull(message = "출근 시간은 필수입니다.")
    private LocalTime clockInTime;
}
