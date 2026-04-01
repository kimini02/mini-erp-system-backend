package com.minierp.backend.domain.task.dto;

import com.minierp.backend.domain.task.entity.TaskAssignment;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskAssignmentResponseDto {

    private Long id;
    private Long taskId;
    private Long userId;

    public static TaskAssignmentResponseDto from(TaskAssignment taskAssignment) {
        return new TaskAssignmentResponseDto(
                taskAssignment.getId(),
                taskAssignment.getTask().getId(),
                taskAssignment.getUser().getId()
        );
    }
}
