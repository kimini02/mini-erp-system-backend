package com.minierp.backend.domain.task.repository;

import com.minierp.backend.domain.task.entity.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    List<TaskAssignment> findByTaskId(Long taskId);

    List<TaskAssignment> findByUserId(Long userId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    void deleteByTaskIdAndUserId(Long taskId, Long userId);

    // 프로젝트 멤버 해제 시 해당 프로젝트 내 모든 Task 배정을 벌크 삭제
    @Modifying
    @Query("delete from TaskAssignment ta where ta.task.project.id = :projectId and ta.user.id = :userId")
    void deleteByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);

    List<TaskAssignment> findTop10ByOrderByCreatedAtDesc();

    List<TaskAssignment> findTop10ByTaskProjectIdInOrderByCreatedAtDesc(List<Long> projectIds);
}
