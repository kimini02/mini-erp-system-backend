package com.minierp.backend.domain.user.dto;

import com.minierp.backend.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserResponseDto {

    private final Long id;
    private final String name;
    private final String email;
    private final String position;
    private final String role;

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUserName(),
                user.getUserEmail(),
                user.getPositionName(),
                user.getUserRole().name()
        );
    }
}
