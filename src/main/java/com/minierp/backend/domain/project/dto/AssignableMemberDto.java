package com.minierp.backend.domain.project.dto;

import com.minierp.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssignableMemberDto {

    private Long userId;
    private String userName;
    private String positionName;

    public static AssignableMemberDto from(User user) {
        return new AssignableMemberDto(
                user.getId(),
                user.getUserName(),
                user.getPositionName()
        );
    }
}
