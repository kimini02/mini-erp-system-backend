package com.minierp.backend.domain.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalTime;

@Getter
public class CheckOutRequestDto {

    @NotNull(message = "퇴근 시간은 필수입니다.")
    private LocalTime clockOutTime;
}

