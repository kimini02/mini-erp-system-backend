package com.minierp.backend.domain.attendance.controller;

import com.minierp.backend.domain.attendance.dto.AttendanceSummaryDto;
import com.minierp.backend.domain.attendance.dto.AttendanceUpdateRequestDto;
import com.minierp.backend.domain.attendance.dto.AttendanceUpdateResponseDto;
import com.minierp.backend.domain.attendance.dto.CheckInRequestDto;
import com.minierp.backend.domain.attendance.dto.CheckInResponseDto;
import com.minierp.backend.domain.attendance.dto.CheckOutRequestDto;
import com.minierp.backend.domain.attendance.dto.CheckOutResponseDto;
import com.minierp.backend.domain.attendance.service.AttendanceService;
import com.minierp.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<CheckInResponseDto>> checkIn(
            @Valid @RequestBody CheckInRequestDto requestDto,
            Authentication authentication
    ) {
        CheckInResponseDto response = attendanceService.checkIn(authentication.getName(), requestDto);
        return ResponseEntity.ok(ApiResponse.success(response, "출근이 기록되었습니다"));
    }

    @PatchMapping("/check-out")
    public ResponseEntity<ApiResponse<CheckOutResponseDto>> checkOut(
            @RequestParam LocalDate workDate,
            @Valid @RequestBody CheckOutRequestDto requestDto,
            Authentication authentication
    ) {
        CheckOutResponseDto response = attendanceService.checkOut(authentication.getName(), workDate, requestDto);
        return ResponseEntity.ok(ApiResponse.success(response, "퇴근이 기록되었습니다"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AttendanceUpdateResponseDto>> updateAttendance(
            @RequestParam LocalDate workDate,
            @Valid @RequestBody AttendanceUpdateRequestDto requestDto,
            Authentication authentication
    ) {
        AttendanceUpdateResponseDto response = attendanceService.updateFullAttendance(authentication.getName(), workDate, requestDto);
        return ResponseEntity.ok(ApiResponse.success(response, "근태 기록이 수정되었습니다"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AttendanceSummaryDto>> getSummary(
            @RequestParam String month,
            Authentication authentication
    ) {
        AttendanceSummaryDto response = attendanceService.getMonthlySummary(authentication.getName(), month);
        return ResponseEntity.ok(ApiResponse.success(response, "근태 요약 조회가 완료되었습니다"));
    }
}
