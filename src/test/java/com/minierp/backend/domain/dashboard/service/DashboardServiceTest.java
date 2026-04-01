package com.minierp.backend.domain.dashboard.service;

import com.minierp.backend.domain.approval.entity.LeaveStatus;
import com.minierp.backend.domain.approval.repository.LeaveRequestRepository;
import com.minierp.backend.domain.dashboard.dto.AdminDashboardResponseDto;
import com.minierp.backend.domain.dashboard.dto.DashboardProjectDto;
import com.minierp.backend.domain.dashboard.dto.DashboardResponseDto;
import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.project.entity.ProjectStatus;
import com.minierp.backend.domain.project.repository.ProjectRepository;
import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.task.entity.TaskPriority;
import com.minierp.backend.domain.task.entity.TaskStatus;
import com.minierp.backend.domain.task.repository.TaskRepository;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = DashboardServiceTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Transactional
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private LeaveRequestRepository leaveRequestRepository;

    @Test
    @DisplayName("대시보드 진행률은 상태별 업무 수와 완료율을 계산한다")
    void getDashboardStats_success() {
        given(taskRepository.findAll()).willReturn(List.of(
                createTask(1L, TaskStatus.TODO),
                createTask(2L, TaskStatus.DOING),
                createTask(3L, TaskStatus.DONE),
                createTask(4L, TaskStatus.DONE)
        ));

        DashboardResponseDto response = dashboardService.getDashboardStats();

        assertThat(response.getTodoCount()).isEqualTo(1L);
        assertThat(response.getDoingCount()).isEqualTo(1L);
        assertThat(response.getDoneCount()).isEqualTo(2L);
        assertThat(response.getProgressRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("ADMIN은 관리자 대시보드 통계를 조회할 수 있다")
    void getAdminSummary_asAdmin_success() {
        given(userRepository.count()).willReturn(30L);
        given(projectRepository.countByStatus(ProjectStatus.PROGRESS)).willReturn(4L);
        given(taskRepository.count()).willReturn(20L);
        given(taskRepository.countByTaskStatus(TaskStatus.DONE)).willReturn(8L);
        given(leaveRequestRepository.countByAppStatus(LeaveStatus.PENDING)).willReturn(3L);

        AdminDashboardResponseDto response = dashboardService.getAdminSummary(1L, UserRole.ADMIN);

        assertThat(response.getTotalUsers()).isEqualTo(30L);
        assertThat(response.getActiveProjectCount()).isEqualTo(4L);
        assertThat(response.getPendingApprovalCount()).isEqualTo(3L);
        assertThat(response.getTotalTaskCount()).isEqualTo(20L);
        assertThat(response.getTaskCompletionRate()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("TEAM_LEADER는 본인 담당 프로젝트 기준으로 관리자 대시보드 통계를 조회한다")
    void getAdminSummary_asTeamLeader_success() {
        Project project1 = createProject(1L, ProjectStatus.PROGRESS);
        Project project2 = createProject(2L, ProjectStatus.READY);
        given(projectRepository.findByLeaderId(10L)).willReturn(List.of(project1, project2));
        given(taskRepository.countByProjectIdIn(List.of(1L, 2L))).willReturn(10L);
        given(taskRepository.countByProjectIdInAndTaskStatus(List.of(1L, 2L), TaskStatus.DONE)).willReturn(5L);
        given(leaveRequestRepository.countByAppStatus(LeaveStatus.PENDING)).willReturn(2L);

        AdminDashboardResponseDto response = dashboardService.getAdminSummary(10L, UserRole.TEAM_LEADER);

        assertThat(response.getTotalUsers()).isZero();
        assertThat(response.getActiveProjectCount()).isEqualTo(1L);
        assertThat(response.getPendingApprovalCount()).isEqualTo(2L);
        assertThat(response.getTotalTaskCount()).isEqualTo(10L);
        assertThat(response.getTaskCompletionRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("USER는 관리자 대시보드 통계를 조회할 수 없다")
    void getAdminSummary_asUser_throwsAccessDenied() {
        assertThatThrownBy(() -> dashboardService.getAdminSummary(1L, UserRole.USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("ADMIN은 프로젝트 현황을 진행 중 우선으로 최대 5개 조회한다")
    void getDashboardProjects_asAdmin_success() {
        Project p1 = createProject(1L, ProjectStatus.READY, LocalDate.of(2026, 4, 30), "A");
        Project p2 = createProject(2L, ProjectStatus.PROGRESS, LocalDate.of(2026, 4, 20), "B");
        Project p3 = createProject(3L, ProjectStatus.PROGRESS, LocalDate.of(2026, 4, 25), "C");
        Project p4 = createProject(4L, ProjectStatus.READY, LocalDate.of(2026, 5, 1), "D");
        Project p5 = createProject(5L, ProjectStatus.PROGRESS, LocalDate.of(2026, 4, 10), "E");
        Project p6 = createProject(6L, ProjectStatus.READY, LocalDate.of(2026, 4, 15), "F");
        given(projectRepository.findAll()).willReturn(List.of(p1, p2, p3, p4, p5, p6));
        given(taskRepository.countByProjectId(1L)).willReturn(2L);
        given(taskRepository.countByProjectIdAndTaskStatus(1L, TaskStatus.DONE)).willReturn(1L);
        given(taskRepository.countByProjectId(2L)).willReturn(2L);
        given(taskRepository.countByProjectIdAndTaskStatus(2L, TaskStatus.DONE)).willReturn(2L);
        given(taskRepository.countByProjectId(3L)).willReturn(4L);
        given(taskRepository.countByProjectIdAndTaskStatus(3L, TaskStatus.DONE)).willReturn(2L);
        given(taskRepository.countByProjectId(4L)).willReturn(1L);
        given(taskRepository.countByProjectIdAndTaskStatus(4L, TaskStatus.DONE)).willReturn(0L);
        given(taskRepository.countByProjectId(5L)).willReturn(5L);
        given(taskRepository.countByProjectIdAndTaskStatus(5L, TaskStatus.DONE)).willReturn(1L);
        given(taskRepository.countByProjectId(6L)).willReturn(3L);
        given(taskRepository.countByProjectIdAndTaskStatus(6L, TaskStatus.DONE)).willReturn(1L);

        List<DashboardProjectDto> responses = dashboardService.getDashboardProjects(1L, UserRole.ADMIN);

        assertThat(responses).hasSize(5);
        assertThat(responses).extracting(DashboardProjectDto::getProjectId)
                .containsExactly(5L, 2L, 3L, 6L, 1L);
    }

    @Test
    @DisplayName("TEAM_LEADER는 본인 담당 프로젝트 현황만 조회한다")
    void getDashboardProjects_asTeamLeader_success() {
        Project p1 = createProject(1L, ProjectStatus.PROGRESS);
        Project p2 = createProject(2L, ProjectStatus.READY);
        given(projectRepository.findByLeaderId(10L)).willReturn(List.of(p1, p2));
        given(taskRepository.countByProjectId(1L)).willReturn(2L);
        given(taskRepository.countByProjectIdAndTaskStatus(1L, TaskStatus.DONE)).willReturn(1L);
        given(taskRepository.countByProjectId(2L)).willReturn(1L);
        given(taskRepository.countByProjectIdAndTaskStatus(2L, TaskStatus.DONE)).willReturn(0L);

        List<DashboardProjectDto> responses = dashboardService.getDashboardProjects(10L, UserRole.TEAM_LEADER);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(DashboardProjectDto::getProjectId)
                .containsExactly(1L, 2L);
    }

    private Task createTask(Long id, TaskStatus taskStatus) {
        Project project = createProject(1L, ProjectStatus.PROGRESS);

        Task task = Task.create(
                "업무 제목",
                "업무 내용",
                LocalDate.of(2026, 4, 2),
                taskStatus,
                TaskPriority.MEDIUM,
                project
        );
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private Project createProject(Long id, ProjectStatus status) {
        return createProject(id, status, LocalDate.of(2026, 4, 30), "ERP 재구축");
    }

    private Project createProject(Long id, ProjectStatus status, LocalDate endDate, String title) {
        Project project;
        try {
            Constructor<Project> constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            project = constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Project 테스트 객체 생성에 실패했습니다.", e);
        }
        ReflectionTestUtils.setField(project, "id", id);
        ReflectionTestUtils.setField(project, "title", title);
        ReflectionTestUtils.setField(project, "content", "설명");
        ReflectionTestUtils.setField(project, "status", status);
        ReflectionTestUtils.setField(project, "startDate", LocalDate.of(2026, 3, 31));
        ReflectionTestUtils.setField(project, "endDate", endDate);
        return project;
    }

    @TestConfiguration
    static class TestTransactionConfig {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }

    @SpringBootConfiguration
    @Import({DashboardService.class, TestTransactionConfig.class})
    static class TestApplication {
    }
}
