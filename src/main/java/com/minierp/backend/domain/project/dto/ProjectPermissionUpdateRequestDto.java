package com.minierp.backend.domain.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectPermissionUpdateRequestDto {

    @NotNull(message = "프로젝트 ID 목록은 필수입니다.")
    private List<Long> assignedProjectIds;

    public static ProjectPermissionUpdateRequestDto of(List<Long> assignedProjectIds) {
        ProjectPermissionUpdateRequestDto dto = new ProjectPermissionUpdateRequestDto();
        dto.assignedProjectIds = assignedProjectIds;
        return dto;
    }
}
