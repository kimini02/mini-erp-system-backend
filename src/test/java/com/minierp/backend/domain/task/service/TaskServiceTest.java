package com.minierp.backend.domain.task.service;

import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.project.repository.ProjectMemberRepository;
import com.minierp.backend.domain.project.repository.ProjectRepository;
import com.minierp.backend.domain.task.dto.TaskAssignmentResponseDto;
import com.minierp.backend.domain.task.dto.TaskCreateRequestDto;
import com.minierp.backend.domain.task.dto.RecentAssignmentDto;
import com.minierp.backend.domain.task.dto.TaskResponseDto;
import com.minierp.backend.domain.task.dto.TaskStatusUpdateDto;
import com.minierp.backend.domain.task.dto.TaskUpdateRequestDto;
import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.task.entity.TaskAssignment;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = TaskServiceTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Transactional
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private TaskAssignmentRepository taskAssignmentRepository;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private ProjectMemberRepository projectMemberRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("관리자는 업무를 생성하고 다중 담당자를 배정할 수 있다")
    void createTask_asAdmin_success() {
        Long projectId = 1L;
        Project project = createProject(projectId);
        User firstUser = createUser(10L);
        User secondUser = createUser(11L);
        TaskCreateRequestDto request = TaskCreateRequestDto.of(
                projectId,
                "내 업무 화면 구현",
                "React 페이지 및 API 연동",
                LocalDate.of(2026, 4, 2),
                TaskStatus.TODO,
                Priority.HIGH,
                List.of(10L, 11L)
        );

        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(userRepository.findById(10L)).willReturn(Optional.of(firstUser));
        given(userRepository.findById(11L)).willReturn(Optional.of(secondUser));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, 10L)).willReturn(true);
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, 11L)).willReturn(true);
        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedTask, "id", 100L);
            return savedTask;
        });

        TaskResponseDto response = taskService.createTask(request, 1L, UserRole.ADMIN);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getProjectId()).isEqualTo(projectId);
        assertThat(response.getTaskTitle()).isEqualTo("내 업무 화면 구현");
        assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(response.getAssignees()).hasSize(2);
        assertThat(response.getAssignees()).extracting(TaskResponseDto.AssigneeSummaryDto::getUserName)
                .containsExactly("사용자10", "사용자11");
    }

    @Test
    @DisplayName("업무 생성 시 중복 담당자가 포함되면 예외가 발생한다")
    void createTask_withDuplicateAssignees_throwsException() {
        TaskCreateRequestDto request = TaskCreateRequestDto.of(
                1L,
                "내 업무 화면 구현",
                "React 페이지 및 API 연동",
                LocalDate.of(2026, 4, 2),
                TaskStatus.TODO,
                Priority.HIGH,
                List.of(10L, 10L)
        );

        assertThatThrownBy(() -> taskService.createTask(request, 1L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_ASSIGNMENT));
    }

    @Test
    @DisplayName("Task 생성 시 프로젝트에 배정되지 않은 사용자를 담당자로 지정하면 예외가 발생한다")
    void createTask_withNonProjectMemberAssignee_throwsAccessDenied() {
        Long projectId = 1L;
        Long userId = 10L;
        Project project = createProject(projectId);
        User user = createUser(userId);
        TaskCreateRequestDto request = TaskCreateRequestDto.of(
                projectId,
                "내 업무 화면 구현",
                "React 페이지 및 API 연동",
                LocalDate.of(2026, 4, 2),
                TaskStatus.TODO,
                Priority.HIGH,
                List.of(userId)
        );

        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).willReturn(false);

        assertThatThrownBy(() -> taskService.createTask(request, 1L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("업무 생성 시 마감일이 프로젝트 종료일보다 늦으면 예외가 발생한다")
    void createTask_withInvalidPeriod_throwsException() {
        Long projectId = 1L;
        Project project = createProject(projectId);
        TaskCreateRequestDto request = TaskCreateRequestDto.of(
                projectId,
                "내 업무 화면 구현",
                "React 페이지 및 API 연동",
                LocalDate.of(2026, 5, 10),
                TaskStatus.TODO,
                Priority.HIGH,
                List.of(10L)
        );

        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));

        assertThatThrownBy(() -> taskService.createTask(request, 1L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TASK_PERIOD));
    }

    @Test
    @DisplayName("관리자는 전체 업무를 조회할 수 있다")
    void getTasks_asAdmin_returnsAllTasks() {
        Task firstTask = createTask(1L, 1L, TaskStatus.TODO);
        Task secondTask = createTask(2L, 1L, TaskStatus.DOING);
        given(taskRepository.findAll()).willReturn(List.of(firstTask, secondTask));

        List<TaskResponseDto> responses = taskService.getTasks(99L, UserRole.ADMIN);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(TaskResponseDto::getId)
                .containsExactly(1L, 2L);
        verify(taskRepository, never()).findByAssigneeUserId(any());
    }

    @Test
    @DisplayName("일반 사용자는 본인에게 배정된 업무만 조회할 수 있다")
    void getTasks_asUser_returnsAssignedTasksOnly() {
        Long currentUserId = 10L;
        Task task = createTask(1L, 1L, TaskStatus.DOING);
        TaskAssignment.create(task, createUser(currentUserId));
        given(taskRepository.findByAssigneeUserId(currentUserId)).willReturn(List.of(task));

        List<TaskResponseDto> responses = taskService.getTasks(currentUserId, UserRole.USER);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("팀장은 본인 담당 프로젝트의 업무만 조회할 수 있다")
    void getTasks_asTeamLeader_returnsOwnProjectTasks() {
        Long leaderId = 20L;
        Project project = createProject(1L);
        ReflectionTestUtils.setField(project, "leader", createUser(leaderId, UserRole.TEAM_LEADER));
        Task task = createTask(1L, 1L, TaskStatus.DOING);
        ReflectionTestUtils.setField(task.getProject(), "leader", createUser(leaderId, UserRole.TEAM_LEADER));

        given(projectRepository.findByLeaderId(leaderId)).willReturn(List.of(project));
        given(taskRepository.findByProjectId(1L)).willReturn(List.of(task));

        List<TaskResponseDto> responses = taskService.getTasks(leaderId, UserRole.TEAM_LEADER);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("일반 사용자는 본인에게 배정되지 않은 업무를 상세 조회할 수 없다")
    void getTask_asUserWithoutAccess_throwsAccessDenied() {
        Long taskId = 1L;
        Long currentUserId = 10L;
        given(taskRepository.findById(taskId)).willReturn(Optional.of(createTask(taskId, 1L, TaskStatus.TODO)));
        given(taskAssignmentRepository.existsByTaskIdAndUserId(taskId, currentUserId)).willReturn(false);

        assertThatThrownBy(() -> taskService.getTask(taskId, currentUserId, UserRole.USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.TASK_ACCESS_DENIED));
    }

    @Test
    @DisplayName("관리자는 업무 제목, 내용, 마감일, 우선순위를 수정할 수 있다")
    void updateTask_asAdmin_success() {
        Long taskId = 1L;
        Task task = createTask(taskId, 1L, TaskStatus.TODO);
        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

        TaskResponseDto response = taskService.updateTask(
                taskId,
                TaskUpdateRequestDto.of(
                        "수정된 제목",
                        "수정된 내용",
                        LocalDate.of(2026, 4, 10),
                        Priority.HIGH
                ),
                1L,
                UserRole.ADMIN
        );

        assertThat(task.getTaskTitle()).isEqualTo("수정된 제목");
        assertThat(task.getTaskContent()).isEqualTo("수정된 내용");
        assertThat(task.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(response.getTaskTitle()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("담당 팀장은 본인 프로젝트 업무를 수정할 수 있다")
    void updateTask_asTeamLeader_success() {
        Long taskId = 1L;
        Long leaderId = 20L;
        Task task = createTask(taskId, 1L, TaskStatus.TODO);
        ReflectionTestUtils.setField(task.getProject(), "leader", createUser(leaderId, UserRole.TEAM_LEADER));
        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(projectRepository.findById(1L)).willReturn(Optional.of(task.getProject()));

        TaskResponseDto response = taskService.updateTask(
                taskId,
                TaskUpdateRequestDto.of(
                        "팀장 수정 제목",
                        "팀장 수정 내용",
                        LocalDate.of(2026, 4, 20),
                        Priority.MEDIUM
                ),
                leaderId,
                UserRole.TEAM_LEADER
        );

        assertThat(task.getTaskTitle()).isEqualTo("팀장 수정 제목");
        assertThat(task.getTaskContent()).isEqualTo("팀장 수정 내용");
        assertThat(task.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(response.getTaskTitle()).isEqualTo("팀장 수정 제목");
    }

    @Test
    @DisplayName("일반 사용자는 업무를 수정할 수 없다")
    void updateTask_asUser_throwsAccessDenied() {
        assertThatThrownBy(() -> taskService.updateTask(
                1L,
                TaskUpdateRequestDto.of(
                        "수정된 제목",
                        "수정된 내용",
                        LocalDate.of(2026, 5, 10),
                        Priority.HIGH
                ),
                10L,
                UserRole.USER
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("다른 프로젝트 팀장은 업무를 수정할 수 없다")
    void updateTask_asOtherTeamLeader_throwsAccessDenied() {
        Long taskId = 1L;
        Long leaderId = 20L;
        Task task = createTask(taskId, 1L, TaskStatus.TODO);
        ReflectionTestUtils.setField(task.getProject(), "leader", createUser(30L, UserRole.TEAM_LEADER));
        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(projectRepository.findById(1L)).willReturn(Optional.of(task.getProject()));

        assertThatThrownBy(() -> taskService.updateTask(
                taskId,
                TaskUpdateRequestDto.of(
                        "수정된 제목",
                        "수정된 내용",
                        LocalDate.of(2026, 5, 10),
                        Priority.HIGH
                ),
                leaderId,
                UserRole.TEAM_LEADER
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("존재하지 않는 업무를 수정하면 예외가 발생한다")
    void updateTask_whenTaskMissing_throwsTaskNotFound() {
        Long taskId = 1L;
        given(taskRepository.findById(taskId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(
                taskId,
                TaskUpdateRequestDto.of(
                        "수정된 제목",
                        "수정된 내용",
                        LocalDate.of(2026, 5, 10),
                        Priority.HIGH
                ),
                1L,
                UserRole.ADMIN
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.TASK_NOT_FOUND));
    }

    @Test
    @DisplayName("업무 수정 시 마감일이 프로젝트 종료일보다 늦으면 예외가 발생한다")
    void updateTask_withInvalidPeriod_throwsException() {
        Long taskId = 1L;
        Task task = createTask(taskId, 1L, TaskStatus.TODO);
        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateTask(
                taskId,
                TaskUpdateRequestDto.of(
                        "수정된 제목",
                        "수정된 내용",
                        LocalDate.of(2026, 5, 10),
                        Priority.HIGH
                ),
                1L,
                UserRole.ADMIN
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TASK_PERIOD));
    }

    @Test
    @DisplayName("배정된 일반 사용자는 자신의 업무 상태를 변경할 수 있다")
    void changeTaskStatus_asAssignedUser_success() {
        Long taskId = 1L;
        Long currentUserId = 10L;
        Task task = createTask(taskId, 1L, TaskStatus.TODO);
        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(taskAssignmentRepository.existsByTaskIdAndUserId(taskId, currentUserId)).willReturn(true);

        TaskResponseDto response = taskService.changeTaskStatus(
                taskId,
                currentUserId,
                UserRole.USER,
                TaskStatusUpdateDto.of(TaskStatus.DONE)
        );

        assertThat(task.getTaskStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(response.getTaskStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    @DisplayName("일반 사용자는 타인의 업무 상태를 변경할 수 없다")
    void changeTaskStatus_asDifferentUser_throwsAccessDenied() {
        Long taskId = 1L;
        Long currentUserId = 10L;
        given(taskRepository.findById(taskId)).willReturn(Optional.of(createTask(taskId, 1L, TaskStatus.TODO)));
        given(taskAssignmentRepository.existsByTaskIdAndUserId(taskId, currentUserId)).willReturn(false);

        assertThatThrownBy(() -> taskService.changeTaskStatus(
                taskId,
                currentUserId,
                UserRole.USER,
                TaskStatusUpdateDto.of(TaskStatus.DONE)
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.TASK_ACCESS_DENIED));
    }

    @Test
    @DisplayName("이미 배정된 담당자를 다시 추가하면 예외가 발생한다")
    void addAssignment_duplicate_throwsException() {
        Long taskId = 1L;
        Long userId = 10L;
        given(taskRepository.findById(taskId)).willReturn(Optional.of(createTask(taskId, 1L, TaskStatus.TODO)));
        given(userRepository.findById(userId)).willReturn(Optional.of(createUser(userId)));
        given(taskAssignmentRepository.existsByTaskIdAndUserId(taskId, userId)).willReturn(true);

        assertThatThrownBy(() -> taskService.addAssignment(taskId, userId, 1L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_ASSIGNMENT));
    }

    @Test
    @DisplayName("프로젝트에 참여한 사용자는 업무 담당자로 추가할 수 있다")
    void addAssignment_asProjectMember_success() {
        Long taskId = 1L;
        Long userId = 10L;
        Task task = createTask(taskId, 1L, TaskStatus.TODO);
        User user = createUser(userId);

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(taskAssignmentRepository.existsByTaskIdAndUserId(taskId, userId)).willReturn(false);
        given(projectMemberRepository.existsByProjectIdAndUserId(task.getProject().getId(), userId)).willReturn(true);
        given(taskAssignmentRepository.save(any(TaskAssignment.class))).willAnswer(invocation -> {
            TaskAssignment savedAssignment = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedAssignment, "id", 101L);
            return savedAssignment;
        });

        TaskAssignmentResponseDto response = taskService.addAssignment(taskId, userId, 1L, UserRole.ADMIN);

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getTaskId()).isEqualTo(taskId);
        assertThat(response.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("프로젝트에 참여하지 않은 사용자를 담당자로 추가하면 예외가 발생한다")
    void addAssignment_whenUserIsNotProjectMember_throwsAccessDenied() {
        Long taskId = 1L;
        Long userId = 10L;
        Task task = createTask(taskId, 1L, TaskStatus.TODO);

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(userRepository.findById(userId)).willReturn(Optional.of(createUser(userId)));
        given(taskAssignmentRepository.existsByTaskIdAndUserId(taskId, userId)).willReturn(false);
        given(projectMemberRepository.existsByProjectIdAndUserId(task.getProject().getId(), userId)).willReturn(false);

        assertThatThrownBy(() -> taskService.addAssignment(taskId, userId, 1L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("배정된 일반 사용자는 업무 담당자 목록을 조회할 수 있다")
    void getAssignments_asAssignedUser_success() {
        Long taskId = 1L;
        Long currentUserId = 10L;
        Task task = createTask(taskId, 1L, TaskStatus.TODO);
        TaskAssignment assignment = TaskAssignment.create(task, createUser(currentUserId));
        ReflectionTestUtils.setField(assignment, "id", 101L);

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(taskAssignmentRepository.existsByTaskIdAndUserId(taskId, currentUserId)).willReturn(true);
        given(taskAssignmentRepository.findByTaskId(taskId)).willReturn(List.of(assignment));

        List<TaskAssignmentResponseDto> responses = taskService.getAssignments(taskId, currentUserId, UserRole.USER);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTaskId()).isEqualTo(taskId);
        assertThat(responses.get(0).getUserId()).isEqualTo(currentUserId);
    }

    @Test
    @DisplayName("존재하지 않는 담당자 배정을 해제하면 예외가 발생한다")
    void removeAssignment_whenMissing_throwsException() {
        Long taskId = 1L;
        Long userId = 10L;
        given(taskRepository.findById(taskId)).willReturn(Optional.of(createTask(taskId, 1L, TaskStatus.TODO)));
        given(userRepository.findById(userId)).willReturn(Optional.of(createUser(userId)));
        given(taskAssignmentRepository.existsByTaskIdAndUserId(taskId, userId)).willReturn(false);

        assertThatThrownBy(() -> taskService.removeAssignment(taskId, userId, 1L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND));

    }

    @Test
    @DisplayName("팀장은 본인 담당 프로젝트에 업무를 생성할 수 있다")
    void createTask_asTeamLeader_success() {
        Long leaderId = 20L;
        Long projectId = 1L;
        Project project = createProject(projectId);
        ReflectionTestUtils.setField(project, "leader", createUser(leaderId, UserRole.TEAM_LEADER));
        User user = createUser(10L, UserRole.USER);
        TaskCreateRequestDto request = TaskCreateRequestDto.of(
                projectId,
                "팀장 업무 생성",
                "팀장 권한 검증",
                LocalDate.of(2026, 4, 2),
                TaskStatus.TODO,
                Priority.MEDIUM,
                List.of(10L)
        );

        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(userRepository.findById(10L)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, 10L)).willReturn(true);
        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedTask, "id", 120L);
            return savedTask;
        });

        TaskResponseDto response = taskService.createTask(request, leaderId, UserRole.TEAM_LEADER);
        assertThat(response.getId()).isEqualTo(120L);
    }

    @Test
    @DisplayName("팀장은 본인 담당이 아닌 프로젝트에 업무를 생성할 수 없다")
    void createTask_asTeamLeaderWithoutOwnership_throwsAccessDenied() {
        Long leaderId = 20L;
        Long projectId = 1L;
        Project project = createProject(projectId);
        ReflectionTestUtils.setField(project, "leader", createUser(30L, UserRole.TEAM_LEADER));
        TaskCreateRequestDto request = TaskCreateRequestDto.of(
                projectId,
                "팀장 업무 생성",
                "팀장 권한 검증",
                LocalDate.of(2026, 4, 2),
                TaskStatus.TODO,
                Priority.MEDIUM,
                List.of(10L)
        );
        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));

        assertThatThrownBy(() -> taskService.createTask(request, leaderId, UserRole.TEAM_LEADER))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("ADMIN은 최근 업무 배정 이력을 조회할 수 있다")
    void getRecentAssignments_asAdmin_success() {
        Task task = createTask(1L, 1L, TaskStatus.TODO);
        User user = createUser(10L, UserRole.USER);
        TaskAssignment assignment = TaskAssignment.create(task, user);
        ReflectionTestUtils.setField(assignment, "id", 100L);

        given(taskAssignmentRepository.findTop10ByOrderByCreatedAtDesc()).willReturn(List.of(assignment));

        List<RecentAssignmentDto> responses = taskService.getRecentAssignments(1L, UserRole.ADMIN);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTaskId()).isEqualTo(1L);
        assertThat(responses.get(0).getAssigneeName()).isEqualTo("사용자10");
    }

    @Test
    @DisplayName("TEAM_LEADER는 본인 담당 프로젝트의 최근 업무 배정 이력만 조회한다")
    void getRecentAssignments_asTeamLeader_success() {
        Long leaderId = 20L;
        Project project = createProject(1L);
        ReflectionTestUtils.setField(project, "leader", createUser(leaderId, UserRole.TEAM_LEADER));
        Task task = Task.create(
                "업무 제목",
                "업무 내용",
                LocalDate.of(2026, 4, 2),
                TaskStatus.TODO,
                Priority.MEDIUM,
                project
        );
        ReflectionTestUtils.setField(task, "id", 1L);

        User user = createUser(10L, UserRole.USER);
        TaskAssignment assignment = TaskAssignment.create(task, user);
        ReflectionTestUtils.setField(assignment, "id", 100L);

        given(projectRepository.findByLeaderId(leaderId)).willReturn(List.of(project));
        given(taskAssignmentRepository.findTop10ByTaskProjectIdInOrderByCreatedAtDesc(List.of(1L)))
                .willReturn(List.of(assignment));

        List<RecentAssignmentDto> responses = taskService.getRecentAssignments(leaderId, UserRole.TEAM_LEADER);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProjectTitle()).isEqualTo("ERP 재구축");
    }

    @Test
    @DisplayName("USER는 최근 업무 배정 이력을 조회할 수 없다")
    void getRecentAssignments_asUser_throwsAccessDenied() {
        assertThatThrownBy(() -> taskService.getRecentAssignments(10L, UserRole.USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    private Project createProject(Long id) {
        Project project = Project.create(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                Priority.MEDIUM
        );
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private Task createTask(Long id, Long projectId, TaskStatus taskStatus) {
        Task task = Task.create(
                "업무 제목",
                "업무 내용",
                LocalDate.of(2026, 4, 2),
                taskStatus,
                Priority.MEDIUM,
                createProject(projectId)
        );
        ReflectionTestUtils.setField(task, "id", id);
        return task;
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
    @Import({TaskService.class, TestTransactionConfig.class})
    static class TestApplication {
    }
}
