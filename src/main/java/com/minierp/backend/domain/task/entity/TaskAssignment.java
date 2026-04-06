package com.minierp.backend.domain.task.entity;

import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업무 배정(TaskAssignment) — Task와 User의 다대다를 중간 테이블로 풀어냄
 * - @ManyToMany 대신 중간 엔티티로 배정 이력(createdAt) 관리
 * - UniqueConstraint로 DB 레벨 중복 배정 방지
 */
@Entity
@Table(name = "task_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"task_id", "user_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TaskAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static TaskAssignment create(Task task, User user) {
        TaskAssignment taskAssignment = new TaskAssignment();
        taskAssignment.task = task;
        taskAssignment.user = user;
        if (task != null) {
            task.addTaskAssignment(taskAssignment);
        }
        return taskAssignment;
    }
}
