package com.minierp.backend.domain.user.dto;

import com.minierp.backend.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserRoleUpdateResponseDto {

    private final Long id;
    private final String role;
    private final LocalDateTime updatedAt;

    public static UserRoleUpdateResponseDto from(User user) {
        return new UserRoleUpdateResponseDto(user.getId(), user.getUserRole().name(), user.getUpdatedAt());
    }
}
