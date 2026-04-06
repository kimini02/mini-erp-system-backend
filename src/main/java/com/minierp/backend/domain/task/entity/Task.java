package com.minierp.backend.domain.task.entity;

import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.global.entity.BaseEntity;
import com.minierp.backend.global.entity.Priority;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 업무(Task) 엔티티
 * - @Setter 금지, 상태 변경은 도메인 메서드(changeStatus, update)로만 수행
 * - Task 마감일은 프로젝트 마감일을 초과할 수 없음 (@PrePersist/@PreUpdate 검증)
 * - TaskAssignment를 통해 다대다 담당자 배정 (중간 테이블)
 */
@Entity
@Table(name = "tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Task extends BaseEntity {

    @Column(name = "task_title", nullable = false, length = 100)
    private String taskTitle;

    @Column(name = "task_content", nullable = false, length = 1000)
    private String taskContent;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false)
    private TaskStatus taskStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Task 삭제 시 배정 정보도 함께 삭제 (데이터 정합성)
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskAssignment> taskAssignments = new ArrayList<>();

    public static Task create(
            String taskTitle,
            String taskContent,
            LocalDate endDate,
            TaskStatus taskStatus,
            Priority priority,
            Project project
    ) {
        Task task = new Task();
        task.taskTitle = taskTitle;
        task.taskContent = taskContent;
        task.endDate = endDate;
        task.taskStatus = taskStatus == null ? TaskStatus.TODO : taskStatus;
        task.priority = priority;
        task.project = project;
        task.validatePeriod();
        return task;
    }

    public void changeStatus(TaskStatus newStatus) {
        this.taskStatus = newStatus;
    }

    public void update(String taskTitle, String taskContent, LocalDate endDate, Priority priority) {
        this.taskTitle = taskTitle;
        this.taskContent = taskContent;
        this.endDate = endDate;
        this.priority = priority;
        this.validatePeriod();
    }

    public boolean isOverdue() {
        return taskStatus != TaskStatus.DONE && endDate != null && endDate.isBefore(LocalDate.now());
    }

    // 양방향 연관관계 편의 메서드 — TaskAssignment.create()에서 호출
    void addTaskAssignment(TaskAssignment taskAssignment) {
        taskAssignments.add(taskAssignment);
    }

    // JPA 콜백: 저장/수정 직전에 Task 마감일이 프로젝트 마감일을 넘지 않는지 검증
    @PrePersist
    @PreUpdate
    private void validatePeriod() {
        if (project != null && project.getEndDate() != null && endDate != null && endDate.isAfter(project.getEndDate())) {
            throw new BusinessException(ErrorCode.INVALID_TASK_PERIOD);
        }
    }
}
