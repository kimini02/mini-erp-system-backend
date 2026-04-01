package com.minierp.backend.domain.task.repository;

import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    long countByProjectId(Long projectId);

    long countByProjectIdAndTaskStatus(Long projectId, TaskStatus taskStatus);
}
