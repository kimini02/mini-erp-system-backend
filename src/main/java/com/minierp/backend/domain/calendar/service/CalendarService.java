package com.minierp.backend.domain.calendar.service;

import com.minierp.backend.domain.approval.entity.LeaveStatus;
import com.minierp.backend.domain.approval.repository.LeaveRequestRepository;
import com.minierp.backend.domain.calendar.dto.CalendarEventResponseDto;
import com.minierp.backend.domain.overtime.entity.OvertimeStatus;
import com.minierp.backend.domain.overtime.repository.OvertimeRequestRepository;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final OvertimeRequestRepository overtimeRequestRepository;
    private final UserRepository userRepository;

    /**
     * 개인 캘린더 이벤트 조회 (연차 + 특근)
     */
    public List<CalendarEventResponseDto> getCalendarEvents(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Long id = user.getId();
        LocalDate[] range = resolveMonthRange(year, month);
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        List<CalendarEventResponseDto> events = new ArrayList<>();
        events.addAll(getApprovedLeaveEvents(id, startDate, endDate));
        events.addAll(getApprovedOvertimeEvents(id, startDate, endDate));

        return events.stream()
                .sorted(Comparator.comparing(CalendarEventResponseDto::getStart))
                .collect(Collectors.toList());
    }

    /**
     * 전사 통합 캘린더 이벤트 조회 (연차 + 특근)
     */
    public List<CalendarEventResponseDto> getCalendarEvents(int year, int month) {
        LocalDate[] range = resolveMonthRange(year, month);
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        List<CalendarEventResponseDto> events = new ArrayList<>();
        events.addAll(getAllApprovedLeaveEvents(startDate, endDate));
        events.addAll(getAllApprovedOvertimeEvents(startDate, endDate));

        return events.stream()
                .sorted(Comparator.comparing(CalendarEventResponseDto::getStart))
                .collect(Collectors.toList());
    }

    private List<CalendarEventResponseDto> getApprovedLeaveEvents(Long userId, LocalDate start, LocalDate end) {
        return leaveRequestRepository
                .findOverlappingLeavesByRequester(userId, LeaveStatus.APPROVED, start, end)
                .stream()
                .map(leave -> CalendarEventResponseDto.builder()
                        .eventId(leave.getId())
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

    private List<CalendarEventResponseDto> getAllApprovedLeaveEvents(LocalDate start, LocalDate end) {
        return leaveRequestRepository
                .findOverlappingLeaves(LeaveStatus.APPROVED, start, end)
                .stream()
                .map(leave -> CalendarEventResponseDto.builder()
                        .eventId(leave.getId())
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
                        .title("[" + overtime.getRequester().getUserName() + "] 특근")
                        .start(overtime.getOvertimeDate().atTime(overtime.getStartTime()))
                        .end(overtime.getOvertimeDate().atTime(overtime.getEndTime()))
                        .type("OVERTIME")
                        .build())
                .collect(Collectors.toList());
    }

    private LocalDate[] resolveMonthRange(int year, int month) {
        try {
            YearMonth ym = YearMonth.of(year, month);
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        } catch (DateTimeException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "year/month 값이 유효하지 않습니다.");
        }
    }
}