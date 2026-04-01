package com.minierp.backend.domain.project.entity;

import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.global.entity.BaseEntity;
import com.minierp.backend.global.entity.Priority;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.minierp.backend.domain.task.entity.TaskStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Project extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String content;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private User leader;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> projectMembers = new ArrayList<>();

    public static Project create(
            String title,
            String content,
            LocalDate startDate,
            LocalDate endDate,
            Priority priority
    ) {
        return create(title, content, startDate, endDate, priority, null);
    }

    public static Project create(
            String title,
            String content,
            LocalDate startDate,
            LocalDate endDate,
            Priority priority,
            User leader
    ) {
        Project project = new Project();
        project.title = title;
        project.content = content;
        project.startDate = startDate;
        project.endDate = endDate;
        project.status = ProjectStatus.READY;
        project.priority = priority == null ? Priority.MEDIUM : priority;
        project.leader = leader;
        project.validatePeriod();
        return project;
    }

    public void changeStatus(ProjectStatus newStatus) {
        this.status = newStatus;
    }

    public void assignLeader(User leader) {
        this.leader = leader;
    }

    public void updateStatusByTasks() {
        if (tasks.isEmpty()) {
            return;
        }
        boolean allDone = tasks.stream()
                .allMatch(task -> task.getTaskStatus() == TaskStatus.DONE);
        if (allDone) {
            this.status = ProjectStatus.DONE;
        } else if (this.status == ProjectStatus.READY) {
            boolean anyStarted = tasks.stream()
                    .anyMatch(task -> task.getTaskStatus() != TaskStatus.TODO);
            if (anyStarted) {
                this.status = ProjectStatus.PROGRESS;
            }
        }
    }

    @PrePersist
    @PreUpdate
    private void validatePeriod() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_PERIOD);
        }
    }
}
