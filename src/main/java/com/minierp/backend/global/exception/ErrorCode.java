package com.minierp.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 요청 값입니다."),
    LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 데이터입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    INVALID_PROJECT_PERIOD(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다."),
    DUPLICATE_PROJECT_MEMBER(HttpStatus.CONFLICT, "이미 프로젝트에 배정된 팀원입니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트 팀원 정보를 찾을 수 없습니다."),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "업무를 찾을 수 없습니다."),
    INVALID_TASK_PERIOD(HttpStatus.BAD_REQUEST, "업무 종료일은 시작일보다 빠를 수 없습니다."),
    DUPLICATE_ASSIGNMENT(HttpStatus.CONFLICT, "이미 배정된 담당자입니다."),
    ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "담당자 배정 정보를 찾을 수 없습니다."),
    TASK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인에게 배정된 업무만 접근할 수 있습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
