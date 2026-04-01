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
import com.minierp.backend.domain.task.repository.TaskAssignmentRepository;
import com.minierp.backend.domain.task.repository.TaskRepository;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponseDto createTask(TaskCreateRequestDto request, Long currentUserId, UserRole currentUserRole) {
        validateAdminOrLeaderRole(currentUserRole);
        if (currentUserRole == UserRole.TEAM_LEADER) {
            validateProjectLeaderForTask(request.getProjectId(), currentUserId);
        }
        validateDuplicateAssigneeIds(request.getAssigneeIds());

        Project project = findProjectOrThrow(request.getProjectId());

        Task task = Task.create(
                request.getTaskTitle(),
                request.getTaskContent(),
                request.getEndDate(),
                request.getTaskStatus(),
                request.getPriority(),
                project
        );

        request.getAssigneeIds().stream()
                .map(userId -> {
                    User user = findUserOrThrow(userId);
                    validateProjectMember(
                            project.getId(),
                            userId,
                            "해당 프로젝트에 배정되지 않은 사용자입니다: " + userId
                    );
                    return user;
                })
                .forEach(user -> TaskAssignment.create(task, user));

        Task savedTask = taskRepository.save(task);
        return TaskResponseDto.from(savedTask);
    }

    public List<TaskResponseDto> getTasks(Long currentUserId, UserRole currentUserRole) {
        if (currentUserRole == UserRole.ADMIN) {
            return taskRepository.findAll().stream()
                    .map(TaskResponseDto::from)
                    .toList();
        }

        validateCurrentUserId(currentUserId);

        if (currentUserRole == UserRole.TEAM_LEADER) {
            List<Project> leaderProjects = projectRepository.findByLeaderId(currentUserId);
            if (leaderProjects.isEmpty()) {
                throw new BusinessException(ErrorCode.NO_ASSIGNED_PROJECT);
            }
            return leaderProjects.stream()
                    .flatMap(project -> taskRepository.findByProjectId(project.getId()).stream())
                    .map(TaskResponseDto::from)
                    .toList();
        }

        return taskRepository.findByAssigneeUserId(currentUserId).stream()
                .map(TaskResponseDto::from)
                .toList();
    }

    public TaskResponseDto getTask(Long taskId, Long currentUserId, UserRole currentUserRole) {
        Task task = findTaskOrThrow(taskId);
        validateTaskAccess(task, currentUserId, currentUserRole);
        return TaskResponseDto.from(task);
    }

    @Transactional
    public TaskResponseDto changeTaskStatus(
            Long taskId,
            Long currentUserId,
            UserRole currentUserRole,
            TaskStatusUpdateDto request
    ) {
        Task task = findTaskOrThrow(taskId);
        if (currentUserRole == UserRole.ADMIN) {
            task.changeStatus(request.getTaskStatus());
            return TaskResponseDto.from(task);
        }
        if (currentUserRole == UserRole.TEAM_LEADER) {
            validateProjectLeaderForTask(task.getProject().getId(), currentUserId);
            task.changeStatus(request.getTaskStatus());
            return TaskResponseDto.from(task);
        }

        validateTaskAccess(task, currentUserId, currentUserRole);
        task.changeStatus(request.getTaskStatus());
        return TaskResponseDto.from(task);
    }

    @Transactional
    public TaskAssignmentResponseDto addAssignment(
            Long taskId,
            Long userId,
            Long currentUserId,
            UserRole currentUserRole
    ) {
        validateAdminOrLeaderRole(currentUserRole);

        Task task = findTaskOrThrow(taskId);
        if (currentUserRole == UserRole.TEAM_LEADER) {
            validateProjectLeaderForTask(task.getProject().getId(), currentUserId);
        }
        User user = findUserOrThrow(userId);

        if (taskAssignmentRepository.existsByTaskIdAndUserId(taskId, userId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ASSIGNMENT);
        }

        validateProjectMember(
                task.getProject().getId(),
                userId,
                "해당 프로젝트에 배정되지 않은 사용자입니다."
        );

        TaskAssignment taskAssignment = TaskAssignment.create(task, user);
        TaskAssignment savedTaskAssignment = taskAssignmentRepository.save(taskAssignment);
        return TaskAssignmentResponseDto.from(savedTaskAssignment);
    }

    public List<TaskAssignmentResponseDto> getAssignments(Long taskId, Long currentUserId, UserRole currentUserRole) {
        Task task = findTaskOrThrow(taskId);
        validateTaskAccess(task, currentUserId, currentUserRole);

        return taskAssignmentRepository.findByTaskId(taskId).stream()
                .map(TaskAssignmentResponseDto::from)
                .toList();
    }

    @Transactional
    public void removeAssignment(Long taskId, Long userId, Long currentUserId, UserRole currentUserRole) {
        validateAdminOrLeaderRole(currentUserRole);

        Task task = findTaskOrThrow(taskId);
        if (currentUserRole == UserRole.TEAM_LEADER) {
            validateProjectLeaderForTask(task.getProject().getId(), currentUserId);
        }
        findUserOrThrow(userId);

        if (!taskAssignmentRepository.existsByTaskIdAndUserId(taskId, userId)) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND);
        }

        taskAssignmentRepository.deleteByTaskIdAndUserId(taskId, userId);
    }

    private void validateAdminRole(UserRole currentUserRole) {
        if (currentUserRole != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateAdminOrLeaderRole(UserRole currentUserRole) {
        if (currentUserRole == UserRole.USER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateCurrentUserId(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "현재 사용자 정보가 필요합니다.");
        }
    }

    private void validateTaskAccess(Task task, Long currentUserId, UserRole currentUserRole) {
        if (currentUserRole == UserRole.ADMIN) {
            return;
        }

        validateCurrentUserId(currentUserId);

        if (currentUserRole == UserRole.TEAM_LEADER) {
            if (task.getProject().getLeader() != null && task.getProject().getLeader().getId().equals(currentUserId)) {
                return;
            }
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (!taskAssignmentRepository.existsByTaskIdAndUserId(task.getId(), currentUserId)) {
            throw new BusinessException(ErrorCode.TASK_ACCESS_DENIED);
        }
    }

    private void validateProjectLeaderForTask(Long projectId, Long currentUserId) {
        validateCurrentUserId(currentUserId);
        Project project = findProjectOrThrow(projectId);
        if (project.getLeader() == null || !project.getLeader().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 프로젝트의 담당 팀장이 아닙니다.");
        }
    }

    private void validateDuplicateAssigneeIds(List<Long> assigneeIds) {
        Set<Long> uniqueAssigneeIds = new LinkedHashSet<>(assigneeIds);
        if (uniqueAssigneeIds.size() != assigneeIds.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_ASSIGNMENT);
        }
    }

    private void validateProjectMember(Long projectId, Long userId, String detailMessage) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, detailMessage);
        }
    }

    private Task findTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
