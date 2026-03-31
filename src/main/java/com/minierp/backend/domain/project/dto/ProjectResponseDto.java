package com.minierp.backend.domain.project.dto;

import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.project.entity.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ProjectResponseDto {

    private Long projectId;
    private String title;
    private String content;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;

    public static ProjectResponseDto from(Project project) {
        return new ProjectResponseDto(
                project.getId(),
                project.getTitle(),
                project.getContent(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate()
        );
    }
}
