package com.minierp.backend.domain.calendar.controller;

import com.minierp.backend.domain.calendar.dto.CalendarEventResponseDto;
import com.minierp.backend.domain.calendar.service.CalendarService;
import com.minierp.backend.global.response.ApiResponse;
import com.minierp.backend.global.security.CurrentUserResolver;
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
    private final CurrentUserResolver currentUserResolver;

    /**
     * 캘린더 통합 이벤트 조회 (연차 + 특근)
     * GET /api/v1/calendar/events?year=2026&month=3
     */
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<CalendarEventResponseDto>>> getCalendarEvents(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month) {

        List<CalendarEventResponseDto> events = calendarService.getCalendarEvents(
                currentUserResolver.resolveUserId(authentication),
                year,
                month
        );
        return ResponseEntity.ok(ApiResponse.success(events, "캘린더 이벤트 조회가 완료되었습니다."));
    }
}