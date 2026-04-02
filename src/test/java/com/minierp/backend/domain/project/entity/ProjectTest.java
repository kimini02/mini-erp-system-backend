package com.minierp.backend.domain.project.entity;

import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.global.entity.Priority;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectTest {

    @Test
    @DisplayName("프로젝트 생성 시 기본 상태는 READY로 저장된다")
    void createProject_success() {
        Project project = Project.create(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                Priority.MEDIUM
        );

        assertThat(project.getTitle()).isEqualTo("ERP 재구축");
        assertThat(project.getContent()).isEqualTo("사내 업무 시스템 고도화");
        assertThat(project.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(project.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.READY);
        assertThat(project.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("프로젝트 생성 시 종료일이 시작일보다 빠르면 예외가 발생한다")
    void createProject_invalidPeriod_throwsException() {
        assertThatThrownBy(() -> Project.create(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 3, 31),
                Priority.HIGH
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PROJECT_PERIOD));
    }

    @Test
    @DisplayName("프로젝트 상태를 변경할 수 있다")
    void changeStatus_success() {
        Project project = Project.create(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                Priority.LOW
        );

        project.changeStatus(ProjectStatus.PROGRESS);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PROGRESS);
    }

    @Test
    @DisplayName("프로젝트 수정 시 제목, 내용, 기간, 우선순위가 변경된다")
    void updateProject_success() {
        Project project = Project.create(
                "기존 제목",
                "기존 내용",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                Priority.LOW
        );

        project.update(
                "수정된 제목",
                "수정된 내용",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 30),
                Priority.HIGH
        );

        assertThat(project.getTitle()).isEqualTo("수정된 제목");
        assertThat(project.getContent()).isEqualTo("수정된 내용");
        assertThat(project.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(project.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(project.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("프로젝트 수정 시 종료일이 시작일보다 빠르면 예외가 발생한다")
    void updateProject_invalidPeriod_throwsException() {
        Project project = Project.create(
                "제목",
                "내용",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                Priority.MEDIUM
        );

        assertThatThrownBy(() -> project.update(
                "제목",
                "내용",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 4, 1),
                Priority.MEDIUM
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PROJECT_PERIOD));
    }

    @Test
    @DisplayName("프로젝트 팀장을 변경할 수 있다")
    void assignLeader_success() {
        Project project = Project.create(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                Priority.MEDIUM
        );
        User leader = createUser(20L, UserRole.TEAM_LEADER);

        project.assignLeader(leader);

        assertThat(project.getLeader()).isEqualTo(leader);
        assertThat(project.getLeader().getId()).isEqualTo(20L);
    }

    private User createUser(Long id, UserRole role) {
        User user;
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            user = constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("User 테스트 객체 생성에 실패했습니다.", e);
        }
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "userName", "사용자" + id);
        ReflectionTestUtils.setField(user, "userRole", role);
        return user;
    }
}
