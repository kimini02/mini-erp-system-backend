package com.minierp.backend.domain.calendar.service;

import com.minierp.backend.domain.approval.entity.LeaveStatus;
import com.minierp.backend.domain.approval.repository.LeaveRequestRepository;
import com.minierp.backend.domain.calendar.dto.CalendarEventResponseDto;
import com.minierp.backend.domain.overtime.entity.OvertimeStatus;
import com.minierp.backend.domain.overtime.repository.OvertimeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final OvertimeRequestRepository overtimeRequestRepository;

    /**
     * 통합 캘린더 이벤트 조회 (연차 + 특근)
     */
    public List<CalendarEventResponseDto> getCalendarEvents(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<CalendarEventResponseDto> events = new ArrayList<>();

        // 1. 승인된 연차(Leave) 내역 조회 및 변환
        events.addAll(getApprovedLeaveEvents(userId, startDate, endDate));

        // 2. 승인된 특근(Overtime) 내역 조회 및 변환
        events.addAll(getApprovedOvertimeEvents(userId, startDate, endDate));

        return events;
    }

    private List<CalendarEventResponseDto> getApprovedLeaveEvents(Long userId, LocalDate start, LocalDate end) {
        return leaveRequestRepository
                .findByRequester_IdAndAppStatusAndStartDateBetween(userId, LeaveStatus.APPROVED, start, end)
                .stream()
                .map(leave -> CalendarEventResponseDto.builder()
                        .eventId(leave.getId())
                        // 연차 엔티티에 별도 사유 필드가 없으므로 유형명으로 표시
                        .title("연차 - " + leave.getAppType().getDisplayName())
                        .start(leave.getStartDate().atStartOfDay())
                        .end(leave.getEndDate().atTime(23, 59, 59))
                        .type("LEAVE")
                        .build())
                .collect(Collectors.toList());
    }

    private List<CalendarEventResponseDto> getApprovedOvertimeEvents(Long userId, LocalDate start, LocalDate end) {
        return overtimeRequestRepository
                .findByRequester_IdAndStatusAndOvertimeDateBetween(userId, OvertimeStatus.APPROVED, start, end)
                .stream()
                .map(overtime -> CalendarEventResponseDto.builder()
                        .eventId(overtime.getId())
                        .title("특근 - " + (overtime.getReason() != null ? overtime.getReason() : "특근 업무"))
                        .start(overtime.getOvertimeDate().atTime(overtime.getStartTime()))
                        .end(overtime.getOvertimeDate().atTime(overtime.getEndTime()))
                        .type("OVERTIME")
                        .build())
                .collect(Collectors.toList());
    }
}
