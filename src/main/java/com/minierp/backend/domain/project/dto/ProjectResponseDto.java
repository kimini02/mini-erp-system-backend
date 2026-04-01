package com.minierp.backend.domain.project.dto;

import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.project.entity.ProjectStatus;
import com.minierp.backend.global.entity.Priority;
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
    private Priority priority;
    private LocalDate startDate;
    private LocalDate endDate;
    private long memberCount;
    private long taskCount;
    private int progressRate;
    private Long leaderId;
    private String leaderName;

    public static ProjectResponseDto from(Project project) {
        return from(project, 0L, 0L, 0);
    }

    public static ProjectResponseDto from(Project project, long memberCount, long taskCount, int progressRate) {
        return new ProjectResponseDto(
                project.getId(),
                project.getTitle(),
                project.getContent(),
                project.getStatus(),
                project.getPriority(),
                project.getStartDate(),
                project.getEndDate(),
                memberCount,
                taskCount,
                progressRate,
                project.getLeader() != null ? project.getLeader().getId() : null,
                project.getLeader() != null ? project.getLeader().getUserName() : null
        );
    }
}
