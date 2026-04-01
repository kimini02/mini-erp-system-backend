package com.minierp.backend.domain.dashboard.dto;

import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.project.entity.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class DashboardProjectDto {

    private Long projectId;
    private String title;
    private ProjectStatus status;
    private int progressRate;
    private LocalDate endDate;

    public static DashboardProjectDto from(Project project, int progressRate) {
        return new DashboardProjectDto(
                project.getId(),
                project.getTitle(),
                project.getStatus(),
                progressRate,
                project.getEndDate()
        );
    }
}
