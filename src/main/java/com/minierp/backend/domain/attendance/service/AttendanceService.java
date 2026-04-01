package com.minierp.backend.domain.attendance.service;

import com.minierp.backend.domain.attendance.dto.AttendanceSummaryDto;
import com.minierp.backend.domain.attendance.dto.CheckInRequestDto;
import com.minierp.backend.domain.attendance.dto.CheckInResponseDto;
import com.minierp.backend.domain.attendance.dto.CheckOutRequestDto;
import com.minierp.backend.domain.attendance.dto.CheckOutResponseDto;
import com.minierp.backend.domain.attendance.dto.AttendanceUpdateRequestDto;
import com.minierp.backend.domain.attendance.dto.AttendanceUpdateResponseDto;
import com.minierp.backend.domain.attendance.entity.Attendance;
import com.minierp.backend.domain.attendance.entity.AttendanceStatus;
import com.minierp.backend.domain.attendance.repository.AttendanceRepository;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @Transactional
    public CheckInResponseDto checkIn(String loginId, CheckInRequestDto requestDto) {
        User user = getUserByLoginId(loginId);
        LocalDate workDate = requestDto.getWorkDate();

        attendanceRepository.findByUserAndWorkDate(user, workDate)
                .ifPresent(attendance -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 출근 기록이 존재합니다.");
                });

        boolean holiday = isHoliday(workDate);
        boolean approvedOvertime = hasApprovedOvertime(user);
        if (holiday && !approvedOvertime) {
            throw new BusinessException(ErrorCode.OVERTIME_APPROVAL_REQUIRED);
        }

        Attendance attendance = Attendance.checkIn(user, workDate, requestDto.getClockInTime());
        Attendance savedAttendance = attendanceRepository.save(attendance);
        return CheckInResponseDto.from(savedAttendance);
    }

    @Transactional
    public CheckOutResponseDto checkOut(String loginId, LocalDate workDate, CheckOutRequestDto requestDto) {
        User user = getUserByLoginId(loginId);

        Attendance attendance = attendanceRepository.findByUserAndWorkDate(user, workDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 날짜의 출근 기록이 없습니다."));

        attendance.checkOut(requestDto.getClockOutTime());
        return CheckOutResponseDto.from(attendance);
    }

    @Transactional
    public AttendanceUpdateResponseDto updateAttendance(String loginId, LocalDate workDate, CheckOutRequestDto checkOutRequestDto) {
        User user = getUserByLoginId(loginId);

        Attendance attendance = attendanceRepository.findByUserAndWorkDate(user, workDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 날짜의 근태 기록이 없습니다."));

        attendance.checkOut(checkOutRequestDto.getClockOutTime());
        return AttendanceUpdateResponseDto.from(attendance);
    }

    @Transactional
    public AttendanceUpdateResponseDto updateFullAttendance(String loginId, LocalDate workDate, AttendanceUpdateRequestDto requestDto) {
        User user = getUserByLoginId(loginId);

        Attendance attendance = attendanceRepository.findByUserAndWorkDate(user, workDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 날짜의 근태 기록이 없습니다."));

        attendance.updateAttendance(requestDto.getClockInTime(), requestDto.getClockOutTime());
        return AttendanceUpdateResponseDto.from(attendance);
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryDto getMonthlySummary(String loginId, String month) {
        User user = getUserByLoginId(loginId);
        YearMonth yearMonth = parseYearMonth(month);

        List<Attendance> attendances = attendanceRepository.findAllByUserAndWorkDateBetweenOrderByWorkDateAsc(
                user,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );

        int workDaysCount = attendances.size();
        List<String> clockInTimes = attendances.stream()
                .filter(a -> a.getClockInTime() != null)
                .map(a -> a.getClockInTime().toString())
                .collect(Collectors.toList());

        List<String> clockOutTimes = attendances.stream()
                .filter(a -> a.getClockOutTime() != null)
                .map(a -> a.getClockOutTime().toString())
                .collect(Collectors.toList());

        int leaveUsedCount = (int) attendances.stream()
                .filter(a -> a.getAttStatus() == AttendanceStatus.LEAVE)
                .count();

        return AttendanceSummaryDto.of(workDaysCount, clockInTimes, clockOutTimes, leaveUsedCount);
    }

    private User getUserByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private boolean isHoliday(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private boolean hasApprovedOvertime(User user) {
        return user.getUserRole() == UserRole.ADMIN || "OVERTIME_APPROVED".equalsIgnoreCase(user.getAssignRole());
    }

    private YearMonth parseYearMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "month는 YYYY-MM 형식이어야 합니다.");
        }
    }
}
