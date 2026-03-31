package com.minierp.backend.domain.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.task.entity.TaskAssignment;
import com.minierp.backend.domain.task.entity.TaskStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class TaskResponseDto {

    private Long id;
    private Long projectId;
    private String taskTitle;
    private String taskContent;

    @JsonProperty("taskState")
    private TaskStatus taskStatus;

    private LocalDate startDate;
    private LocalDate endDate;
    private String taskNo;
    private List<AssigneeSummaryDto> assignees;

    public static TaskResponseDto from(Task task) {
        return new TaskResponseDto(
                task.getId(),
                task.getProject().getId(),
                task.getTaskTitle(),
                task.getTaskContent(),
                task.getTaskStatus(),
                task.getStartDate(),
                task.getEndDate(),
                task.getTaskNo(),
                task.getTaskAssignments().stream()
                        .map(AssigneeSummaryDto::from)
                        .toList()
        );
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AssigneeSummaryDto {

        private Long id;

        public static AssigneeSummaryDto from(TaskAssignment taskAssignment) {
            return new AssigneeSummaryDto(taskAssignment.getUser().getId());
        }
    }
}
