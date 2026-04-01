package com.minierp.backend.domain.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventResponseDto {
    private Long eventId;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private String type; // "LEAVE", "OVERTIME"
}
