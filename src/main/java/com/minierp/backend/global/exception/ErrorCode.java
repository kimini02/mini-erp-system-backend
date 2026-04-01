package com.minierp.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    
    // Overtime
    OVERTIME_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "특근 신청 내역을 찾을 수 없습니다."),
    INVALID_OVERTIME_TIME(HttpStatus.BAD_REQUEST, "O002", "종료 시간이 시작 시간보다 빠를 수 없습니다."),
    OVERTIME_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "O003", "이미 승인 또는 반려된 요청입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
