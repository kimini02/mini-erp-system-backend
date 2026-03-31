package com.minierp.backend.domain.attendance.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AttendanceSummaryDto {

    private final int workDaysCount;
    private final List<String> clockInTimes;
    private final List<String> clockOutTimes;
    private final int leaveUsedCount;

    public static AttendanceSummaryDto of(int workDaysCount, List<String> clockInTimes, List<String> clockOutTimes, int leaveUsedCount) {
        return new AttendanceSummaryDto(workDaysCount, clockInTimes, clockOutTimes, leaveUsedCount);
    }
}
