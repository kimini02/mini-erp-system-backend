package com.minierp.backend.domain.project.service;

import com.minierp.backend.domain.project.dto.ProjectCreateRequestDto;
import com.minierp.backend.domain.project.dto.ProjectPermissionDto;
import com.minierp.backend.domain.project.dto.ProjectPermissionUpdateRequestDto;
import com.minierp.backend.domain.project.dto.ProjectMemberResponseDto;
import com.minierp.backend.domain.project.dto.ProjectProgressResponseDto;
import com.minierp.backend.domain.project.dto.ProjectResponseDto;
import com.minierp.backend.domain.project.dto.ProjectUpdateRequestDto;
import com.minierp.backend.domain.project.dto.AssignableMemberDto;
import com.minierp.backend.domain.project.dto.LeaderSummaryDto;
import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.project.entity.ProjectMember;
import com.minierp.backend.domain.project.repository.ProjectMemberRepository;
import com.minierp.backend.domain.project.repository.ProjectRepository;
import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.global.entity.Priority;
import com.minierp.backend.domain.task.entity.TaskStatus;
import com.minierp.backend.domain.task.repository.TaskAssignmentRepository;
import com.minierp.backend.domain.task.repository.TaskRepository;
import com.minierp.backend.domain.user.entity.User;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = ProjectServiceTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Transactional
class ProjectServiceTest {

    @Autowired
    private ProjectService projectService;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private ProjectMemberRepository projectMemberRepository;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private TaskAssignmentRepository taskAssignmentRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("관리자는 프로젝트를 생성할 수 있다")
    void createProject_asAdmin_success() {
        Long leaderId = 20L;
        User leader = createUser(leaderId, UserRole.TEAM_LEADER);
        ProjectCreateRequestDto request = ProjectCreateRequestDto.of(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                Priority.MEDIUM,
                leaderId
        );
        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
            Project savedProject = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedProject, "id", 1L);
            return savedProject;
        });

        ProjectResponseDto response = projectService.createProject(request, UserRole.ADMIN);

        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("ERP 재구축");
        assertThat(response.getStatus().name()).isEqualTo("READY");
        assertThat(response.getLeaderId()).isEqualTo(leaderId);
    }

    @Test
    @DisplayName("일반 사용자는 프로젝트를 생성할 수 없다")
    void createProject_asUser_throwsAccessDenied() {
        ProjectCreateRequestDto request = ProjectCreateRequestDto.of(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                Priority.MEDIUM,
                null
        );

        assertThatThrownBy(() -> projectService.createProject(request, UserRole.USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("팀장이 아닌 사용자를 프로젝트 팀장으로 지정하면 예외가 발생한다")
    void createProject_withInvalidLeaderRole_throwsException() {
        Long leaderId = 30L;
        ProjectCreateRequestDto request = ProjectCreateRequestDto.of(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                Priority.HIGH,
                leaderId
        );
        given(userRepository.findById(leaderId)).willReturn(Optional.of(createUser(leaderId, UserRole.USER)));

        assertThatThrownBy(() -> projectService.createProject(request, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_LEADER_ROLE));
    }

    @Test
    @DisplayName("관리자는 전체 프로젝트를 조회할 수 있다")
    void getProjects_asAdmin_returnsAllProjects() {
        Project firstProject = createProject(1L, "ERP 재구축");
        Project secondProject = createProject(2L, "그룹웨어 고도화");
        given(projectRepository.findAll()).willReturn(List.of(firstProject, secondProject));
        given(projectMemberRepository.findByProjectId(1L)).willReturn(List.of());
        given(projectMemberRepository.findByProjectId(2L)).willReturn(List.of());
        given(taskRepository.findByProjectId(1L)).willReturn(List.of());
        given(taskRepository.findByProjectId(2L)).willReturn(List.of());

        List<ProjectResponseDto> responses = projectService.getProjects(99L, UserRole.ADMIN);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(ProjectResponseDto::getProjectId)
                .containsExactly(1L, 2L);
        verify(projectMemberRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("일반 사용자는 본인에게 배정된 프로젝트만 조회할 수 있다")
    void getProjects_asUser_returnsAssignedProjectsOnly() {
        Long currentUserId = 10L;
        User user = createUser(currentUserId);
        Project firstProject = createProject(1L, "ERP 재구축");
        Project secondProject = createProject(2L, "그룹웨어 고도화");
        ProjectMember firstMember = createProjectMember(100L, firstProject, user);
        ProjectMember secondMember = createProjectMember(101L, secondProject, user);
        given(projectMemberRepository.findByUserId(currentUserId)).willReturn(List.of(firstMember, secondMember));
        given(projectMemberRepository.findByProjectId(1L)).willReturn(List.of(firstMember));
        given(projectMemberRepository.findByProjectId(2L)).willReturn(List.of(secondMember));
        given(taskRepository.findByProjectId(1L)).willReturn(List.of());
        given(taskRepository.findByProjectId(2L)).willReturn(List.of());

        List<ProjectResponseDto> responses = projectService.getProjects(currentUserId, UserRole.USER);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(ProjectResponseDto::getProjectId)
                .containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("관리자는 프로젝트 제목, 내용, 기간, 우선순위를 수정할 수 있다")
    void updateProject_asAdmin_success() {
        Long projectId = 1L;
        Project project = createProject(projectId, "기존 제목");
        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectId(projectId)).willReturn(List.of(
                createProjectMember(100L, project, createUser(10L)),
                createProjectMember(101L, project, createUser(11L))
        ));
        given(taskRepository.findByProjectId(projectId)).willReturn(List.of(
                createTask(1L, projectId, TaskStatus.TODO),
                createTask(2L, projectId, TaskStatus.DONE)
        ));

        ProjectResponseDto response = projectService.updateProject(
                projectId,
                ProjectUpdateRequestDto.of(
                        "수정된 제목",
                        "수정된 내용",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 6, 30),
                        Priority.HIGH
                ),
                UserRole.ADMIN
        );

        assertThat(project.getTitle()).isEqualTo("수정된 제목");
        assertThat(project.getContent()).isEqualTo("수정된 내용");
        assertThat(project.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(project.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(project.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(response.getTitle()).isEqualTo("수정된 제목");
        assertThat(response.getMemberCount()).isEqualTo(2L);
        assertThat(response.getTaskCount()).isEqualTo(2L);
        assertThat(response.getProgressRate()).isEqualTo(50);
    }

    @Test
    @DisplayName("팀장은 프로젝트를 수정할 수 없다")
    void updateProject_asTeamLeader_throwsAccessDenied() {
        assertThatThrownBy(() -> projectService.updateProject(
                1L,
                ProjectUpdateRequestDto.of(
                        "수정된 제목",
                        "수정된 내용",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 6, 30),
                        Priority.HIGH
                ),
                UserRole.TEAM_LEADER
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("일반 사용자는 프로젝트를 수정할 수 없다")
    void updateProject_asUser_throwsAccessDenied() {
        assertThatThrownBy(() -> projectService.updateProject(
                1L,
                ProjectUpdateRequestDto.of(
                        "수정된 제목",
                        "수정된 내용",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 6, 30),
                        Priority.HIGH
                ),
                UserRole.USER
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트를 수정하면 예외가 발생한다")
    void updateProject_whenProjectMissing_throwsProjectNotFound() {
        Long projectId = 1L;
        given(projectRepository.findById(projectId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProject(
                projectId,
                ProjectUpdateRequestDto.of(
                        "수정된 제목",
                        "수정된 내용",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 6, 30),
                        Priority.HIGH
                ),
                UserRole.ADMIN
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.PROJECT_NOT_FOUND));
    }

    @Test
    @DisplayName("관리자는 프로젝트 팀장을 변경할 수 있다")
    void updateProjectLeader_asAdmin_success() {
        Long projectId = 1L;
        Long leaderId = 20L;
        Project project = createProject(projectId, "ERP 재구축");
        User leader = createUser(leaderId, UserRole.TEAM_LEADER);
        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));
        given(projectMemberRepository.findByProjectId(projectId)).willReturn(List.of(
                createProjectMember(100L, project, createUser(10L))
        ));
        given(taskRepository.findByProjectId(projectId)).willReturn(List.of(
                createTask(1L, projectId, TaskStatus.DONE),
                createTask(2L, projectId, TaskStatus.DONE)
        ));

        ProjectResponseDto response = projectService.updateProjectLeader(projectId, leaderId, UserRole.ADMIN);

        assertThat(project.getLeader()).isEqualTo(leader);
        assertThat(response.getLeaderId()).isEqualTo(leaderId);
        assertThat(response.getLeaderName()).isEqualTo("사용자20");
        assertThat(response.getMemberCount()).isEqualTo(1L);
        assertThat(response.getTaskCount()).isEqualTo(2L);
        assertThat(response.getProgressRate()).isEqualTo(100);
    }

    @Test
    @DisplayName("팀장이 아닌 사용자를 프로젝트 팀장으로 변경하면 예외가 발생한다")
    void updateProjectLeader_withInvalidLeaderRole_throwsException() {
        Long projectId = 1L;
        Long leaderId = 20L;
        given(projectRepository.findById(projectId)).willReturn(Optional.of(createProject(projectId, "ERP 재구축")));
        given(userRepository.findById(leaderId)).willReturn(Optional.of(createUser(leaderId, UserRole.USER)));

        assertThatThrownBy(() -> projectService.updateProjectLeader(projectId, leaderId, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_LEADER_ROLE));
    }

    @Test
    @DisplayName("관리자가 아니면 프로젝트 팀장을 변경할 수 없다")
    void updateProjectLeader_asTeamLeader_throwsAccessDenied() {
        assertThatThrownBy(() -> projectService.updateProjectLeader(1L, 20L, UserRole.TEAM_LEADER))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트의 팀장을 변경하면 예외가 발생한다")
    void updateProjectLeader_whenProjectMissing_throwsProjectNotFound() {
        Long projectId = 1L;
        given(projectRepository.findById(projectId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProjectLeader(projectId, 20L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.PROJECT_NOT_FOUND));
    }

    @Test
    @DisplayName("관리자는 프로젝트 팀원을 배정할 수 있다")
    void addMember_asAdmin_success() {
        Long projectId = 1L;
        Long userId = 10L;
        Project project = createProject(projectId, "ERP 재구축");
        User user = createUser(userId);

        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).willReturn(false);
        given(projectMemberRepository.save(any(ProjectMember.class))).willAnswer(invocation -> {
            ProjectMember savedMember = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedMember, "id", 100L);
            return savedMember;
        });

        ProjectMemberResponseDto response = projectService.addMember(projectId, userId, 1L, UserRole.ADMIN);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getProjectId()).isEqualTo(projectId);
        assertThat(response.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("이미 배정된 프로젝트 팀원을 다시 추가하면 예외가 발생한다")
    void addMember_duplicate_throwsException() {
        Long projectId = 1L;
        Long userId = 10L;
        given(projectRepository.findById(projectId)).willReturn(Optional.of(createProject(projectId, "ERP 재구축")));
        given(userRepository.findById(userId)).willReturn(Optional.of(createUser(userId)));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).willReturn(true);

        assertThatThrownBy(() -> projectService.addMember(projectId, userId, 1L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_PROJECT_MEMBER));
    }

    @Test
    @DisplayName("관리자는 프로젝트 팀원을 해제할 수 있다")
    void removeMember_asAdmin_success() {
        Long projectId = 1L;
        Long userId = 10L;
        given(projectRepository.findById(projectId)).willReturn(Optional.of(createProject(projectId, "ERP 재구축")));
        given(userRepository.findById(userId)).willReturn(Optional.of(createUser(userId)));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).willReturn(true);

        projectService.removeMember(projectId, userId, 1L, UserRole.ADMIN);

        verify(taskAssignmentRepository).deleteByProjectIdAndUserId(projectId, userId);
        verify(projectMemberRepository).deleteByProjectIdAndUserId(projectId, userId);
    }

    @Test
    @DisplayName("배정되지 않은 프로젝트 팀원을 삭제하면 예외가 발생한다")
    void removeMember_whenMemberMissing_throwsException() {
        Long projectId = 1L;
        Long userId = 10L;
        given(projectRepository.findById(projectId)).willReturn(Optional.of(createProject(projectId, "ERP 재구축")));
        given(userRepository.findById(userId)).willReturn(Optional.of(createUser(userId)));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).willReturn(false);

        assertThatThrownBy(() -> projectService.removeMember(projectId, userId, 1L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("프로젝트 진행률은 완료된 업무 비율로 계산한다")
    void getProjectProgress_success() {
        Long projectId = 1L;
        given(projectRepository.findById(projectId)).willReturn(Optional.of(createProject(projectId, "ERP 재구축")));
        given(taskRepository.findByProjectId(projectId)).willReturn(List.of(
                createTask(1L, projectId, TaskStatus.TODO),
                createTask(2L, projectId, TaskStatus.DONE),
                createTask(3L, projectId, TaskStatus.DONE)
        ));

        ProjectProgressResponseDto response = projectService.getProjectProgress(projectId, 99L, UserRole.ADMIN);

        assertThat(response.getProjectId()).isEqualTo(projectId);
        assertThat(response.getTotalTasks()).isEqualTo(3L);
        assertThat(response.getDoneTasks()).isEqualTo(2L);
        assertThat(response.getProgressRate()).isEqualTo(66);
    }

    @Test
    @DisplayName("팀장은 본인 담당 프로젝트에만 팀원을 추가할 수 있다")
    void addMember_asTeamLeader_success() {
        Long projectId = 1L;
        Long leaderId = 20L;
        Long userId = 10L;
        Project project = createProject(projectId, "ERP 재구축");
        ReflectionTestUtils.setField(project, "leader", createUser(leaderId, UserRole.TEAM_LEADER));
        User user = createUser(userId, UserRole.USER);

        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).willReturn(false);
        given(projectMemberRepository.save(any(ProjectMember.class))).willAnswer(invocation -> {
            ProjectMember savedMember = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedMember, "id", 101L);
            return savedMember;
        });

        ProjectMemberResponseDto response = projectService.addMember(projectId, userId, leaderId, UserRole.TEAM_LEADER);

        assertThat(response.getProjectId()).isEqualTo(projectId);
        assertThat(response.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("팀장은 본인 담당이 아닌 프로젝트에 팀원을 추가할 수 없다")
    void addMember_asTeamLeaderWithoutOwnership_throwsAccessDenied() {
        Long projectId = 1L;
        Long leaderId = 20L;
        Project project = createProject(projectId, "ERP 재구축");
        ReflectionTestUtils.setField(project, "leader", createUser(30L, UserRole.TEAM_LEADER));
        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.addMember(projectId, 10L, leaderId, UserRole.TEAM_LEADER))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("업무가 없는 프로젝트의 진행률은 0이다")
    void getProjectProgress_whenNoTasks_returnsZero() {
        Long projectId = 1L;
        given(projectRepository.findById(projectId)).willReturn(Optional.of(createProject(projectId, "ERP 재구축")));
        given(taskRepository.findByProjectId(projectId)).willReturn(List.of());

        ProjectProgressResponseDto response = projectService.getProjectProgress(projectId, 99L, UserRole.ADMIN);

        assertThat(response.getTotalTasks()).isZero();
        assertThat(response.getDoneTasks()).isZero();
        assertThat(response.getProgressRate()).isZero();
    }

    @Test
    @DisplayName("일반 사용자는 본인에게 배정되지 않은 프로젝트 진행률을 조회할 수 없다")
    void getProjectProgress_asUserWithoutAccess_throwsAccessDenied() {
        Long projectId = 1L;
        Long currentUserId = 10L;
        given(projectRepository.findById(projectId)).willReturn(Optional.of(createProject(projectId, "ERP 재구축")));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId)).willReturn(false);

        assertThatThrownBy(() -> projectService.getProjectProgress(projectId, currentUserId, UserRole.USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("ADMIN은 사용자 프로젝트 권한 목록을 전체 프로젝트 기준으로 조회한다")
    void getUserProjectPermissions_asAdmin_success() {
        Long targetUserId = 10L;
        Project firstProject = createProject(1L, "ERP");
        Project secondProject = createProject(2L, "그룹웨어");
        User targetUser = createUser(targetUserId);
        ProjectMember projectMember = createProjectMember(100L, secondProject, targetUser);
        given(projectRepository.findAll()).willReturn(List.of(firstProject, secondProject));
        given(projectMemberRepository.findByUserId(targetUserId)).willReturn(List.of(projectMember));

        List<ProjectPermissionDto> responses = projectService.getUserProjectPermissions(
                targetUserId,
                1L,
                UserRole.ADMIN
        );

        assertThat(responses).hasSize(2);
        assertThat(responses.stream().filter(ProjectPermissionDto::isAssigned).map(ProjectPermissionDto::getProjectId))
                .containsExactly(2L);
    }

    @Test
    @DisplayName("TEAM_LEADER는 본인 담당 프로젝트 범위에서 사용자 권한 목록을 조회한다")
    void getUserProjectPermissions_asTeamLeader_success() {
        Long targetUserId = 10L;
        Long leaderId = 20L;
        Project managedProject = createProject(1L, "ERP");
        ReflectionTestUtils.setField(managedProject, "leader", createUser(leaderId, UserRole.TEAM_LEADER));
        User targetUser = createUser(targetUserId);
        ProjectMember projectMember = createProjectMember(200L, managedProject, targetUser);
        given(projectRepository.findByLeaderId(leaderId)).willReturn(List.of(managedProject));
        given(projectMemberRepository.findByUserId(targetUserId)).willReturn(List.of(projectMember));

        List<ProjectPermissionDto> responses = projectService.getUserProjectPermissions(
                targetUserId,
                leaderId,
                UserRole.TEAM_LEADER
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isAssigned()).isTrue();
    }

    @Test
    @DisplayName("ADMIN은 사용자 프로젝트 권한을 일괄 저장할 수 있다")
    void updateUserProjectPermissions_asAdmin_success() {
        Long targetUserId = 10L;
        User targetUser = createUser(targetUserId, UserRole.USER);
        Project project1 = createProject(1L, "ERP");
        Project project2 = createProject(2L, "그룹웨어");
        Project project3 = createProject(3L, "모바일");

        ProjectMember currentMember1 = createProjectMember(100L, project1, targetUser);
        ProjectMember currentMember2 = createProjectMember(101L, project2, targetUser);

        given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
        given(projectRepository.findAll()).willReturn(List.of(project1, project2, project3));
        given(projectMemberRepository.findByUserId(targetUserId)).willReturn(List.of(currentMember1, currentMember2));
        given(projectRepository.findById(3L)).willReturn(Optional.of(project3));

        projectService.updateUserProjectPermissions(
                targetUserId,
                ProjectPermissionUpdateRequestDto.of(List.of(2L, 3L)),
                1L,
                UserRole.ADMIN
        );

        verify(projectMemberRepository).save(any(ProjectMember.class));
        verify(taskAssignmentRepository).deleteByProjectIdAndUserId(1L, targetUserId);
        verify(projectMemberRepository).deleteByProjectIdAndUserId(1L, targetUserId);
    }

    @Test
    @DisplayName("TEAM_LEADER는 USER만 권한 일괄 저장할 수 있다")
    void updateUserProjectPermissions_asTeamLeaderWithNonUser_throwsAccessDenied() {
        Long targetUserId = 10L;
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(createUser(targetUserId, UserRole.TEAM_LEADER)));

        assertThatThrownBy(() -> projectService.updateUserProjectPermissions(
                targetUserId,
                ProjectPermissionUpdateRequestDto.of(List.of(1L)),
                20L,
                UserRole.TEAM_LEADER
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("TEAM_LEADER가 관리 범위 밖 프로젝트 권한을 설정하면 예외가 발생한다")
    void updateUserProjectPermissions_withOutOfScopeProject_throwsAccessDenied() {
        Long targetUserId = 10L;
        Long leaderId = 20L;
        User targetUser = createUser(targetUserId, UserRole.USER);
        Project managedProject = createProject(1L, "ERP");
        ReflectionTestUtils.setField(managedProject, "leader", createUser(leaderId, UserRole.TEAM_LEADER));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
        given(projectRepository.findByLeaderId(leaderId)).willReturn(List.of(managedProject));

        assertThatThrownBy(() -> projectService.updateUserProjectPermissions(
                targetUserId,
                ProjectPermissionUpdateRequestDto.of(List.of(1L, 2L)),
                leaderId,
                UserRole.TEAM_LEADER
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("배정 가능한 팀원 목록 조회 시 USER 역할만 반환한다")
    void getAssignableMembers_returnsOnlyUserRole() {
        Long projectId = 1L;
        Long leaderId = 20L;
        Project project = createProject(projectId, "ERP");
        ReflectionTestUtils.setField(project, "leader", createUser(leaderId, UserRole.TEAM_LEADER));

        ProjectMember userMember = createProjectMember(1L, project, createUser(10L, UserRole.USER));
        ProjectMember leaderMember = createProjectMember(2L, project, createUser(11L, UserRole.TEAM_LEADER));
        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectId(projectId)).willReturn(List.of(userMember, leaderMember));

        List<AssignableMemberDto> responses = projectService.getAssignableMembers(projectId, leaderId, UserRole.TEAM_LEADER);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("ADMIN은 팀장 목록과 담당 프로젝트 수를 조회할 수 있다")
    void getLeaders_asAdmin_success() {
        User leader1 = createUser(20L, UserRole.TEAM_LEADER);
        User leader2 = createUser(21L, UserRole.TEAM_LEADER);
        given(userRepository.findByUserRole(UserRole.TEAM_LEADER)).willReturn(List.of(leader1, leader2));
        given(projectRepository.countByLeaderId(20L)).willReturn(2L);
        given(projectRepository.countByLeaderId(21L)).willReturn(1L);

        List<LeaderSummaryDto> responses = projectService.getLeaders(UserRole.ADMIN);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(LeaderSummaryDto::getAssignedProjectCount)
                .containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("USER는 팀장 목록을 조회할 수 없다")
    void getLeaders_asUser_throwsAccessDenied() {
        assertThatThrownBy(() -> projectService.getLeaders(UserRole.USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    private Project createProject(Long id, String title) {
        Project project = Project.create(
                title,
                "설명",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                Priority.MEDIUM
        );
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private User createUser(Long id) {
        return createUser(id, UserRole.USER);
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

    private ProjectMember createProjectMember(Long id, Project project, User user) {
        ProjectMember projectMember = ProjectMember.create(project, user);
        ReflectionTestUtils.setField(projectMember, "id", id);
        return projectMember;
    }

    private Task createTask(Long id, Long projectId, TaskStatus taskStatus) {
        Project project = createProject(projectId, "ERP 재구축");
        Task task = Task.create(
                "업무 제목",
                "업무 내용",
                LocalDate.of(2026, 4, 2),
                taskStatus,
                Priority.MEDIUM,
                project
        );
        ReflectionTestUtils.setField(task, "id", id);
        return task;
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
    @Import({ProjectService.class, TestTransactionConfig.class})
    static class TestApplication {
    }
}
