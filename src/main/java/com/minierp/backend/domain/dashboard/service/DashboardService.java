package com.minierp.backend.domain.dashboard.service;

import com.minierp.backend.domain.dashboard.dto.DashboardResponseDto;
import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.task.entity.TaskStatus;
import com.minierp.backend.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TaskRepository taskRepository;

    public DashboardResponseDto getDashboardStats() {
        Map<TaskStatus, Long> taskStatusStats = taskRepository.findAll().stream()
                .collect(groupingBy(
                        Task::getTaskStatus,
                        () -> new EnumMap<>(TaskStatus.class),
                        counting()
                ));

        return DashboardResponseDto.of(taskStatusStats);
    }
}
