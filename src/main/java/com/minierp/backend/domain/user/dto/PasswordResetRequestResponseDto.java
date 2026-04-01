package com.minierp.backend.domain.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordResetRequestResponseDto {

    private final String message;

    public static PasswordResetRequestResponseDto success() {
        return new PasswordResetRequestResponseDto("인증번호가 발송되었습니다");
    }
}

