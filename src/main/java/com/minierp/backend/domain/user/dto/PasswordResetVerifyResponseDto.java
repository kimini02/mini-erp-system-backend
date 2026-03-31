package com.minierp.backend.domain.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordResetVerifyResponseDto {

    private final String resetProof;
    private final String message;

    public static PasswordResetVerifyResponseDto of(String resetProof) {
        return new PasswordResetVerifyResponseDto(resetProof, "인증번호가 확인되었습니다");
    }
}

