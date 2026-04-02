package com.minierp.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.minierp.backend.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {

    private final Long id;
    private final String name;
    private final String email;
    private final String position;
    private final String role;
    private final BigDecimal remainingAnnualLeave;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static UserResponseDto summary(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUserName(),
                user.getUserEmail(),
                user.getPositionName(),
                user.getUserRole().name(),
                null,
                null,
                null
        );
    }

    public static UserResponseDto detail(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUserName(),
                user.getUserEmail(),
                user.getPositionName(),
                user.getUserRole().name(),
                user.getRemainingAnnualLeave(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static UserResponseDto from(User user) {
        return summary(user);
    }
}
