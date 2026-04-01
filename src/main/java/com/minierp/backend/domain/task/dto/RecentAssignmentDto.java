package com.minierp.backend.domain.task.dto;

import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.task.entity.TaskAssignment;
import com.minierp.backend.global.entity.Priority;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RecentAssignmentDto {

    private Long taskId;
    private String taskTitle;
    private String projectTitle;
    private String assigneeName;
    private LocalDate endDate;
    private Priority priority;
    private LocalDateTime createdAt;

    public static RecentAssignmentDto from(TaskAssignment assignment) {
        Task task = assignment.getTask();
        return new RecentAssignmentDto(
                task.getId(),
                task.getTaskTitle(),
                task.getProject().getTitle(),
                assignment.getUser().getUserName(),
                task.getEndDate(),
                task.getPriority(),
                assignment.getCreatedAt()
        );
    }
}
