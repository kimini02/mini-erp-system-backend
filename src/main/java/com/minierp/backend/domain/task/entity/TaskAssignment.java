package com.minierp.backend.domain.task.entity;

import com.minierp.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"task_id", "user_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TaskAssignment extends BaseEntity {
}
