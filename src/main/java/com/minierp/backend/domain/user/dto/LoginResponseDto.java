package com.minierp.backend.domain.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponseDto {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserResponseDto user;

    public static LoginResponseDto of(String accessToken, long expiresIn, UserResponseDto user) {
        return new LoginResponseDto(accessToken, "Bearer", expiresIn, user);
    }
}
