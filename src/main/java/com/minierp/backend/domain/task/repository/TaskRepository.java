package com.minierp.backend.domain.task.repository;

import com.minierp.backend.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
