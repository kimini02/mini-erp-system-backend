package com.minierp.backend.domain.project.dto;

import com.minierp.backend.global.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectUpdateRequestDto {

    @NotBlank(message = "프로젝트 제목은 필수입니다.")
    private String title;

    private String content;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    @NotNull(message = "우선순위는 필수입니다.")
    private Priority priority;

    public static ProjectUpdateRequestDto of(
            String title,
            String content,
            LocalDate startDate,
            LocalDate endDate,
            Priority priority
    ) {
        ProjectUpdateRequestDto dto = new ProjectUpdateRequestDto();
        dto.title = title;
        dto.content = content;
        dto.startDate = startDate;
        dto.endDate = endDate;
        dto.priority = priority;
        return dto;
    }
}
