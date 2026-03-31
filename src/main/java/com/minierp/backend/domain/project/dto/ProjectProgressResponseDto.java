package com.minierp.backend.domain.project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectProgressResponseDto {

    private Long projectId;
    private long totalTasks;
    private long doneTasks;
    private int progressRate;

    public static ProjectProgressResponseDto of(Long projectId, long totalTasks, long doneTasks, int progressRate) {
        return new ProjectProgressResponseDto(projectId, totalTasks, doneTasks, progressRate);
    }
}
