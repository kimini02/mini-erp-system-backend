package com.minierp.backend.domain.task.repository;

import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Task 리포지토리
 * - @EntityGraph: Task + 담당자를 1개 쿼리로 즉시 로딩 (N+1 해결)
 * - GROUP BY 집계 쿼리: 대시보드/프로젝트 목록에서 상태별 개수만 DB에서 반환 (메모리 최적화)
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    @Query("select distinct ta.task from TaskAssignment ta where ta.user.id = :userId")
    List<Task> findByAssigneeUserId(@Param("userId") Long userId);

    long countByTaskStatus(TaskStatus taskStatus);

    long countByProjectId(Long projectId);

    long countByProjectIdAndTaskStatus(Long projectId, TaskStatus taskStatus);

    long countByProjectIdIn(List<Long> projectIds);

    long countByProjectIdInAndTaskStatus(List<Long> projectIds, TaskStatus taskStatus);

    // N+1 해결: @EntityGraph로 담당자(TaskAssignment→User)를 JOIN으로 즉시 로딩
    // distinct: LEFT JOIN으로 인한 중복 제거

    @EntityGraph(attributePaths = {"taskAssignments", "taskAssignments.user"})
    @Query("select distinct t from Task t")
    List<Task> findAllWithAssignees();

    @EntityGraph(attributePaths = {"taskAssignments", "taskAssignments.user"})
    @Query("select distinct t from Task t where t.project.id in :projectIds")
    List<Task> findByProjectIdInWithAssignees(@Param("projectIds") List<Long> projectIds);

    @EntityGraph(attributePaths = {"taskAssignments", "taskAssignments.user"})
    @Query("select distinct t from Task t join t.taskAssignments ta where ta.user.id = :userId")
    List<Task> findByAssigneeUserIdWithAssignees(@Param("userId") Long userId);

    // 프로젝트 목록 1+3N → GROUP BY 1개 쿼리로 일괄 집계

    @Query("select t.project.id, t.taskStatus, count(t) from Task t where t.project.id in :projectIds group by t.project.id, t.taskStatus")
    List<Object[]> countTaskStatusByProjectIds(@Param("projectIds") List<Long> projectIds);

    // 대시보드: findAll()+stream 대신 DB에서 상태별 개수만 반환

    @Query("select t.taskStatus, count(t) from Task t group by t.taskStatus")
    List<Object[]> countByTaskStatusGrouped();

    @Query("select t.taskStatus, count(t) from Task t where t.project.id in :projectIds group by t.taskStatus")
    List<Object[]> countByTaskStatusGroupedForProjects(@Param("projectIds") List<Long> projectIds);

    @Query("select t.taskStatus, count(t) from Task t join t.taskAssignments ta where ta.user.id = :userId group by t.taskStatus")
    List<Object[]> countByTaskStatusGroupedForUser(@Param("userId") Long userId);
}
