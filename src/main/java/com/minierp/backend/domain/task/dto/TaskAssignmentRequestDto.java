package com.minierp.backend.domain.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskAssignmentRequestDto {

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    public static TaskAssignmentRequestDto of(Long userId) {
        TaskAssignmentRequestDto dto = new TaskAssignmentRequestDto();
        dto.userId = userId;
        return dto;
    }
}
