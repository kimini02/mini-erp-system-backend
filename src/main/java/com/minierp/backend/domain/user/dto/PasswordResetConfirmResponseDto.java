package com.minierp.backend.domain.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordResetConfirmResponseDto {

    private final String message;

    public static PasswordResetConfirmResponseDto success() {
        return new PasswordResetConfirmResponseDto("비밀번호가 변경되었습니다. 다시 로그인해주세요");
    }
}

