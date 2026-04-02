package com.minierp.backend.domain.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProjectLeaderUpdateRequestDto {

    @NotNull(message = "팀장 ID는 필수입니다.")
    private Long leaderId;

    public static ProjectLeaderUpdateRequestDto of(Long leaderId) {
        ProjectLeaderUpdateRequestDto dto = new ProjectLeaderUpdateRequestDto();
        dto.leaderId = leaderId;
        return dto;
    }
}
