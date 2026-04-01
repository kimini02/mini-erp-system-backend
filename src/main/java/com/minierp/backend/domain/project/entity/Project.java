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

/**
 * 프로젝트 엔티티
 * - 프로젝트 생성 시 상태는 READY로 시작
 * - 담당 팀장(leader)은 선택 사항, TEAM_LEADER 역할의 User만 가능
 * - Task 상태 변경 시 프로젝트 상태가 자동으로 갱신됨 (READY → PROGRESS → DONE)
 */
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

    // 프로젝트와 업무(Task)가 공유하는 우선순위 (HIGH, MEDIUM, LOW)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    // 담당 팀장 (TEAM_LEADER 역할만 배정 가능)
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

    /**
     * Task 상태 변경 시 프로젝트 상태를 자동으로 갱신
     * - 모든 Task가 DONE → 프로젝트 DONE
     * - READY 상태에서 Task가 하나라도 진행되면 → PROGRESS
     */
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
