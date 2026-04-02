package com.minierp.backend.domain.task.dto;

import com.minierp.backend.global.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskUpdateRequestDto {

    @NotBlank(message = "업무 제목은 필수입니다.")
    private String taskTitle;

    @NotBlank(message = "업무 내용은 필수입니다.")
    private String taskContent;

    @NotNull(message = "업무 종료일은 필수입니다.")
    private LocalDate endDate;

    @NotNull(message = "우선순위는 필수입니다.")
    private Priority priority;

    public static TaskUpdateRequestDto of(
            String taskTitle,
            String taskContent,
            LocalDate endDate,
            Priority priority
    ) {
        TaskUpdateRequestDto dto = new TaskUpdateRequestDto();
        dto.taskTitle = taskTitle;
        dto.taskContent = taskContent;
        dto.endDate = endDate;
        dto.priority = priority;
        return dto;
    }
}
