package com.minierp.backend.global.response;

import com.minierp.backend.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private final boolean success;
    private final ErrorBody error;
    private final LocalDateTime timestamp;

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, new ErrorBody(errorCode.name(), errorCode.getMessage()), LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, new ErrorBody(errorCode.name(), message), LocalDateTime.now());
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ErrorBody {
        private final String code;
        private final String message;
    }
}
