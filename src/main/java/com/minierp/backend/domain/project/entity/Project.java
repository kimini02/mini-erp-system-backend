package com.minierp.backend.domain.project.entity;

import com.minierp.backend.domain.task.entity.Task;
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

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> projectMembers = new ArrayList<>();

    public static Project create(String title, String content, LocalDate startDate, LocalDate endDate) {
        Project project = new Project();
        project.title = title;
        project.content = content;
        project.startDate = startDate;
        project.endDate = endDate;
        project.status = ProjectStatus.READY;
        project.validatePeriod();
        return project;
    }

    public void changeStatus(ProjectStatus newStatus) {
        this.status = newStatus;
    }

    @PrePersist
    @PreUpdate
    private void validatePeriod() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_PERIOD);
        }
    }
}
