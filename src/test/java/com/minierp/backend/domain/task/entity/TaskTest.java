package com.minierp.backend.domain.task.entity;

import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.global.entity.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTest {

    @Test
    @DisplayName("업무 생성 시 상태가 없으면 TODO로 저장된다")
    void createTask_success() {
        Project project = createProject();

        Task task = Task.create(
                "내 업무 화면 구현",
                "React 페이지 및 API 연동",
                LocalDate.of(2026, 4, 2),
                null,
                Priority.HIGH,
                project
        );

        assertThat(task.getTaskTitle()).isEqualTo("내 업무 화면 구현");
        assertThat(task.getTaskContent()).isEqualTo("React 페이지 및 API 연동");
        assertThat(task.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(task.getProject()).isSameAs(project);
    }

    @Test
    @DisplayName("업무 상태를 변경할 수 있다")
    void changeStatus_success() {
        Task task = Task.create(
                "내 업무 화면 구현",
                "React 페이지 및 API 연동",
                LocalDate.of(2026, 4, 2),
                TaskStatus.TODO,
                Priority.MEDIUM,
                createProject()
        );

        task.changeStatus(TaskStatus.DOING);

        assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.DOING);
    }

    @Test
    @DisplayName("업무 수정 시 제목, 내용, 마감일, 우선순위가 변경된다")
    void updateTask_success() {
        Task task = Task.create(
                "기존 제목",
                "기존 내용",
                LocalDate.of(2026, 4, 30),
                TaskStatus.TODO,
                Priority.LOW,
                createProject()
        );

        task.update(
                "수정된 제목",
                "수정된 내용",
                LocalDate.of(2026, 5, 15),
                Priority.HIGH
        );

        assertThat(task.getTaskTitle()).isEqualTo("수정된 제목");
        assertThat(task.getTaskContent()).isEqualTo("수정된 내용");
        assertThat(task.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 15));
        assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("완료되지 않았고 마감일이 지난 업무는 지연 상태로 판단한다")
    void isOverdue_whenTaskIsPastDueAndNotDone_returnsTrue() {
        Task task = Task.create(
                "내 업무 화면 구현",
                "React 페이지 및 API 연동",
                LocalDate.now().minusDays(1),
                TaskStatus.DOING,
                Priority.LOW,
                createProject()
        );

        assertThat(task.isOverdue()).isTrue();
    }

    private Project createProject() {
        return Project.create(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                Priority.MEDIUM
        );
    }
}
