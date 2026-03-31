package com.minierp.backend.domain.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FindIdResponseDto {

    private final String loginId;
    private final String message;

    public static FindIdResponseDto of(String loginId) {
        return new FindIdResponseDto(loginId, "아이디를 찾았습니다");
    }
}
