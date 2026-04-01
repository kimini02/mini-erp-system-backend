package com.minierp.backend.domain.project.entity;

import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.global.entity.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMemberTest {

    @Test
    @DisplayName("프로젝트 팀원 생성 시 프로젝트와 사용자가 연결된다")
    void createProjectMember_success() throws Exception {
        Project project = Project.create(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                Priority.MEDIUM
        );
        User user = createUser();

        ProjectMember projectMember = ProjectMember.create(project, user);

        assertThat(projectMember.getProject()).isSameAs(project);
        assertThat(projectMember.getUser()).isSameAs(user);
    }

    private User createUser() throws Exception {
        Constructor<User> constructor = User.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
