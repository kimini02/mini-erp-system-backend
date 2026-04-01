package com.minierp.backend.domain.dashboard.dto;

import com.minierp.backend.domain.task.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class DashboardResponseDto {

    private long todoCount;
    private long doingCount;
    private long doneCount;
    private double progressRate;

    public static DashboardResponseDto of(Map<TaskStatus, Long> taskStatusStats) {
        long todoCount = taskStatusStats.getOrDefault(TaskStatus.TODO, 0L);
        long doingCount = taskStatusStats.getOrDefault(TaskStatus.DOING, 0L);
        long doneCount = taskStatusStats.getOrDefault(TaskStatus.DONE, 0L);
        long totalCount = todoCount + doingCount + doneCount;
        double progressRate = totalCount == 0 ? 0.0 : (doneCount * 100.0) / totalCount;

        return new DashboardResponseDto(todoCount, doingCount, doneCount, progressRate);
    }
}
