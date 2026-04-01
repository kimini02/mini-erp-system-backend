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
     * 전사 통합 캘린더 이벤트 조회
     * GET /api/v1/calendar/events?year=2026&month=3
     */
    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponseDto>> getCalendarEvents(
            @RequestParam int year,
            @RequestParam int month) {
        
        // 전사 공유 달력이므로 userId 파라미터나 헤더 없이 모든 승인 내역 조회
        List<CalendarEventResponseDto> events = calendarService.getCalendarEvents(year, month);
        return ResponseEntity.ok(events);
    }
}
