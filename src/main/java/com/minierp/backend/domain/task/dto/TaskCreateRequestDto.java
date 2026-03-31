package com.minierp.backend.domain.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.minierp.backend.domain.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskCreateRequestDto {

    @NotNull(message = "프로젝트 ID는 필수입니다.")
    private Long projectId;

    @NotBlank(message = "업무 제목은 필수입니다.")
    private String taskTitle;

    @NotBlank(message = "업무 내용은 필수입니다.")
    private String taskContent;

    @NotNull(message = "업무 시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "업무 종료일은 필수입니다.")
    private LocalDate endDate;

    @JsonProperty("taskState")
    private TaskStatus taskStatus;

    private String taskNo;

    @NotEmpty(message = "담당자 ID 목록은 비어 있을 수 없습니다.")
    private List<Long> assigneeIds;

    public static TaskCreateRequestDto of(
            Long projectId,
            String taskTitle,
            String taskContent,
            LocalDate startDate,
            LocalDate endDate,
            TaskStatus taskStatus,
            String taskNo,
            List<Long> assigneeIds
    ) {
        TaskCreateRequestDto dto = new TaskCreateRequestDto();
        dto.projectId = projectId;
        dto.taskTitle = taskTitle;
        dto.taskContent = taskContent;
        dto.startDate = startDate;
        dto.endDate = endDate;
        dto.taskStatus = taskStatus;
        dto.taskNo = taskNo;
        dto.assigneeIds = assigneeIds;
        return dto;
    }
}
