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
    OVERTIME_APPROVAL_REQUIRED(HttpStatus.FORBIDDEN, "주말/공휴일 출근은 특근 승인 사용자만 가능합니다."),
    LEAVE_DATE_NOT_WORKING_DAY(HttpStatus.UNPROCESSABLE_ENTITY, "주말/공휴일이 포함된 기간은 신청할 수 없습니다."),
    LEAVE_ALREADY_PROCESSED(HttpStatus.UNPROCESSABLE_ENTITY, "이미 처리된 결재 건입니다."),
    SELF_APPROVAL_NOT_ALLOWED(HttpStatus.FORBIDDEN, "본인이 신청한 건은 본인이 승인/반려할 수 없습니다."),
    APPROVAL_ADMIN_ONLY(HttpStatus.FORBIDDEN, "승인/반려는 관리자만 처리할 수 있습니다."),
    REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "반려 사유를 입력해주세요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    OVERTIME_NOT_FOUND(HttpStatus.NOT_FOUND, "특근 신청 정보를 찾을 수 없습니다."),
    INVALID_OVERTIME_TIME(HttpStatus.BAD_REQUEST, "특근 종료 시간은 시작 시간보다 빠를 수 없습니다."),
    INVALID_OVERTIME_DATE(HttpStatus.BAD_REQUEST, "평일에는 특근을 신청할 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 데이터입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_RESET_REQUEST(HttpStatus.BAD_REQUEST, "유효하지 않은 비밀번호 재설정 요청입니다."),
    OTP_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증번호입니다."),
    OTP_EXPIRED(HttpStatus.UNAUTHORIZED, "인증번호가 만료되었습니다."),
    RESET_PROOF_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 재설정 인증입니다."),
    PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "비밀번호 정책을 만족하지 않습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "너무 많은 요청입니다. 잠시 후 다시 시도해주세요."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    NO_ASSIGNED_PROJECT(HttpStatus.FORBIDDEN, "배정된 프로젝트가 없습니다. 관리소장에게 프로젝트 배정을 요청하세요."),
    LEADER_NOT_FOUND(HttpStatus.NOT_FOUND, "팀장 정보를 찾을 수 없습니다."),
    INVALID_LEADER_ROLE(HttpStatus.BAD_REQUEST, "팀장 역할을 가진 사용자만 담당 팀장으로 지정할 수 있습니다."),
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
