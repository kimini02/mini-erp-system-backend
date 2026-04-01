package com.minierp.backend.domain.task.service;

import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.project.repository.ProjectMemberRepository;
import com.minierp.backend.domain.project.repository.ProjectRepository;
import com.minierp.backend.domain.task.dto.TaskAssignmentResponseDto;
import com.minierp.backend.domain.task.dto.TaskCreateRequestDto;
import com.minierp.backend.domain.task.dto.TaskResponseDto;
import com.minierp.backend.domain.task.dto.TaskStatusUpdateDto;
import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.task.entity.TaskAssignment;
import com.minierp.backend.domain.task.entity.TaskPriority;
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
                TaskPriority.HIGH,
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

        TaskResponseDto response = taskService.createTask(request, UserRole.ADMIN);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getProjectId()).isEqualTo(projectId);
        assertThat(response.getTaskTitle()).isEqualTo("내 업무 화면 구현");
        assertThat(response.getPriority()).isEqualTo(TaskPriority.HIGH);
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
                TaskPriority.HIGH,
                List.of(10L, 10L)
        );

        assertThatThrownBy(() -> taskService.createTask(request, UserRole.ADMIN))
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
                TaskPriority.HIGH,
                List.of(userId)
        );

        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).willReturn(false);

        assertThatThrownBy(() -> taskService.createTask(request, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
        verify(taskRepository, never()).save(any(Task.class));
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

        assertThatThrownBy(() -> taskService.addAssignment(taskId, userId, UserRole.ADMIN))
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

        TaskAssignmentResponseDto response = taskService.addAssignment(taskId, userId, UserRole.ADMIN);

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

        assertThatThrownBy(() -> taskService.addAssignment(taskId, userId, UserRole.ADMIN))
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

        assertThatThrownBy(() -> taskService.removeAssignment(taskId, userId, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND));
    }

    private Project createProject(Long id) {
        Project project = Project.create(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30)
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
                TaskPriority.MEDIUM,
                createProject(projectId)
        );
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private User createUser(Long id) {
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
