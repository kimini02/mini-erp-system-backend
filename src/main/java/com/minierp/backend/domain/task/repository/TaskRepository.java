package com.minierp.backend.domain.task.repository;

import com.minierp.backend.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    @Query("select distinct ta.task from TaskAssignment ta where ta.user.id = :userId")
    List<Task> findByAssigneeUserId(@Param("userId") Long userId);
}
