package com.minierp.backend.domain.task.repository;

import com.minierp.backend.domain.task.entity.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
}
