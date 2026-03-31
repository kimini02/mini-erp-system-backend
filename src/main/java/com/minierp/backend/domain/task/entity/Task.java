package com.minierp.backend.domain.task.entity;

import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.global.entity.BaseEntity;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Task extends BaseEntity {

    @Column(name = "task_title", nullable = false, length = 100)
    private String taskTitle;

    @Column(name = "task_content", nullable = false, length = 1000)
    private String taskContent;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false)
    private TaskStatus taskStatus;

    @Column(name = "task_no", length = 50)
    private String taskNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskAssignment> taskAssignments = new ArrayList<>();

    public static Task create(
            String taskTitle,
            String taskContent,
            LocalDate startDate,
            LocalDate endDate,
            TaskStatus taskStatus,
            String taskNo,
            Project project
    ) {
        Task task = new Task();
        task.taskTitle = taskTitle;
        task.taskContent = taskContent;
        task.startDate = startDate;
        task.endDate = endDate;
        task.taskStatus = taskStatus == null ? TaskStatus.TODO : taskStatus;
        task.taskNo = taskNo;
        task.project = project;
        task.validatePeriod();
        return task;
    }

    public void changeStatus(TaskStatus newStatus) {
        this.taskStatus = newStatus;
    }

    public boolean isOverdue() {
        return taskStatus != TaskStatus.DONE && endDate != null && endDate.isBefore(LocalDate.now());
    }

    void addTaskAssignment(TaskAssignment taskAssignment) {
        taskAssignments.add(taskAssignment);
    }

    @PrePersist
    @PreUpdate
    private void validatePeriod() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_TASK_PERIOD);
        }
    }
}
