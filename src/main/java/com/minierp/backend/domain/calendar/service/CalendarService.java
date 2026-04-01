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
     * 전사 통합 캘린더 이벤트 조회 (연차 + 특근)
     * 이제 사용자 필터링 없이 승인된 모든 내역을 가져옵니다.
     */
    public List<CalendarEventResponseDto> getCalendarEvents(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<CalendarEventResponseDto> events = new ArrayList<>();

        // 1. 승인된 모든 연차(Leave) 내역 조회 및 변환
        events.addAll(getAllApprovedLeaveEvents(startDate, endDate));

        // 2. 승인된 모든 특근(Overtime) 내역 조회 및 변환
        events.addAll(getAllApprovedOvertimeEvents(startDate, endDate));

        return events;
    }

    private List<CalendarEventResponseDto> getAllApprovedLeaveEvents(LocalDate start, LocalDate end) {
        return leaveRequestRepository
                .findByAppStatusAndStartDateBetween(LeaveStatus.APPROVED, start, end)
                .stream()
                .map(leave -> CalendarEventResponseDto.builder()
                        .eventId(leave.getId())
                        // [변경] 누구의 일정인지 알 수 있도록 이름 포함 (프라이버시를 위해 사유 제거)
                        .title("[" + leave.getRequester().getUserName() + "] 연차")
                        .start(leave.getStartDate().atStartOfDay())
                        .end(leave.getEndDate().atTime(23, 59, 59))
                        .type("LEAVE")
                        .build())
                .collect(Collectors.toList());
    }

    private List<CalendarEventResponseDto> getAllApprovedOvertimeEvents(LocalDate start, LocalDate end) {
        return overtimeRequestRepository
                .findByStatusAndOvertimeDateBetween(OvertimeStatus.APPROVED, start, end)
                .stream()
                .map(overtime -> CalendarEventResponseDto.builder()
                        .eventId(overtime.getId())
                        // [변경] 누구의 일정인지 알 수 있도록 이름 포함 (프라이버시를 위해 사유 제거)
                        .title("[" + overtime.getRequester().getUserName() + "] 특근")
                        .start(overtime.getOvertimeDate().atTime(overtime.getStartTime()))
                        .end(overtime.getOvertimeDate().atTime(overtime.getEndTime()))
                        .type("OVERTIME")
                        .build())
                .collect(Collectors.toList());
    }
}
