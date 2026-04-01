package com.minierp.backend.domain.task.entity;

import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TaskAssignmentTest {

    @Test
    @DisplayName("담당자 배정 생성 시 Task와 User가 연결된다")
    void createTaskAssignment_success() throws Exception {
        Task task = Task.create(
                "내 업무 화면 구현",
                "React 페이지 및 API 연동",
                LocalDate.of(2026, 4, 2),
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                Project.create(
                        "ERP 재구축",
                        "사내 업무 시스템 고도화",
                        LocalDate.of(2026, 3, 31),
                        LocalDate.of(2026, 4, 30)
                )
        );
        User user = createUser();

        TaskAssignment taskAssignment = TaskAssignment.create(task, user);

        assertThat(taskAssignment.getTask()).isSameAs(task);
        assertThat(taskAssignment.getUser()).isSameAs(user);
        assertThat(task.getTaskAssignments()).contains(taskAssignment);
    }

    private User createUser() throws Exception {
        Constructor<User> constructor = User.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
