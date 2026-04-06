package com.minierp.backend.domain.project.entity;

import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 멤버(ProjectMember) — 프로젝트 접근 권한 테이블
 * - 이 테이블에 레코드가 있어야 해당 프로젝트의 Task에 배정 가능 (권한 = 멤버십)
 */
@Entity
@Table(name = "project_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProjectMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static ProjectMember create(Project project, User user) {
        ProjectMember pm = new ProjectMember();
        pm.project = project;
        pm.user = user;
        return pm;
    }
}
