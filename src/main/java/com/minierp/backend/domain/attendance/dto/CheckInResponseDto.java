package com.minierp.backend.domain.attendance.dto;

import com.minierp.backend.domain.attendance.entity.Attendance;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CheckInResponseDto {

    private final LocalDate workDate;
    private final LocalTime clockInTime;
    private final String attStatus;

    public static CheckInResponseDto from(Attendance attendance) {
        return new CheckInResponseDto(
                attendance.getWorkDate(),
                attendance.getClockInTime(),
                attendance.getAttStatus().name()
        );
    }
}

