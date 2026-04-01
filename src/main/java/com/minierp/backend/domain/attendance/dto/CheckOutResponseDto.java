package com.minierp.backend.domain.attendance.dto;

import com.minierp.backend.domain.attendance.entity.Attendance;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CheckOutResponseDto {

    private final LocalDate workDate;
    private final LocalTime clockInTime;
    private final LocalTime clockOutTime;
    private final String attStatus;

    public static CheckOutResponseDto from(Attendance attendance) {
        return new CheckOutResponseDto(
                attendance.getWorkDate(),
                attendance.getClockInTime(),
                attendance.getClockOutTime(),
                attendance.getAttStatus().name()
        );
    }
}

