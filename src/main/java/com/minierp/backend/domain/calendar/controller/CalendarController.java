package com.minierp.backend.domain.calendar.controller;

import com.minierp.backend.domain.calendar.dto.CalendarEventResponseDto;
import com.minierp.backend.domain.calendar.service.CalendarService;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * 캘린더 통합 이벤트 조회 (연차 + 특근)
     * GET /api/v1/calendar/events?year=2026&month=3
     */
    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponseDto>> getCalendarEvents(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month) {

        List<CalendarEventResponseDto> events = calendarService.getCalendarEvents(extractUserId(authentication), year, month);
        return ResponseEntity.ok(events);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증 사용자 정보가 올바르지 않습니다.");
        }
    }
}