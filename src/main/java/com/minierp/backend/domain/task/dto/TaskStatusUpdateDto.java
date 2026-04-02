package com.minierp.backend.domain.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.minierp.backend.domain.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskStatusUpdateDto {

    @NotNull(message = "업무 상태는 필수입니다.")
    @JsonProperty("taskState")
    private TaskStatus taskStatus;

    public static TaskStatusUpdateDto of(TaskStatus taskStatus) {
        TaskStatusUpdateDto dto = new TaskStatusUpdateDto();
        dto.taskStatus = taskStatus;
        return dto;
    }
}
