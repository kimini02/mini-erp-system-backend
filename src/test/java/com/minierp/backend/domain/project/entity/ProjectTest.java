package com.minierp.backend.domain.project.entity;

import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
                LocalDate.of(2026, 4, 30)
        );

        assertThat(project.getTitle()).isEqualTo("ERP 재구축");
        assertThat(project.getContent()).isEqualTo("사내 업무 시스템 고도화");
        assertThat(project.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(project.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.READY);
    }

    @Test
    @DisplayName("프로젝트 생성 시 종료일이 시작일보다 빠르면 예외가 발생한다")
    void createProject_invalidPeriod_throwsException() {
        assertThatThrownBy(() -> Project.create(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 3, 31)
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
                LocalDate.of(2026, 4, 30)
        );

        project.changeStatus(ProjectStatus.PROGRESS);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PROGRESS);
    }
}
