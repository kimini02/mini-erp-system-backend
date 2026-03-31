package com.minierp.backend.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectCreateRequestDto {

    @NotBlank(message = "프로젝트명은 필수입니다.")
    private String title;

    private String content;

    @NotNull(message = "프로젝트 시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "프로젝트 종료일은 필수입니다.")
    private LocalDate endDate;

    public static ProjectCreateRequestDto of(
            String title,
            String content,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ProjectCreateRequestDto dto = new ProjectCreateRequestDto();
        dto.title = title;
        dto.content = content;
        dto.startDate = startDate;
        dto.endDate = endDate;
        return dto;
    }
}
