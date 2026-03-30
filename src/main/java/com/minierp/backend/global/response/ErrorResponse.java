package com.minierp.backend.global.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private final int statusCode;
    private final String message;
    private final LocalDateTime timestamp;

    public static ErrorResponse of(int statusCode, String message) {
        return new ErrorResponse(statusCode, message, LocalDateTime.now());
    }
}
