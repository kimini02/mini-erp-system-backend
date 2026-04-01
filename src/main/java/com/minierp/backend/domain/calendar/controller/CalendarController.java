package com.minierp.backend.domain.calendar.controller;

import com.minierp.backend.domain.calendar.dto.CalendarEventResponseDto;
import com.minierp.backend.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        
        List<CalendarEventResponseDto> events = calendarService.getCalendarEvents(userId, year, month);
        return ResponseEntity.ok(events);
    }
}
