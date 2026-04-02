package com.minierp.backend.domain.project.dto;

import com.minierp.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AvailableMemberResponseDto {

    private Long userId;
    private String userName;

    public static AvailableMemberResponseDto from(User user) {
        return new AvailableMemberResponseDto(user.getId(), user.getUserName());
    }
}
