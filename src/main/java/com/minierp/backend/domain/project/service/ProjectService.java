package com.minierp.backend.domain.project.service;

import com.minierp.backend.domain.project.dto.ProjectCreateRequestDto;
import com.minierp.backend.domain.project.dto.ProjectMemberResponseDto;
import com.minierp.backend.domain.project.dto.ProjectProgressResponseDto;
import com.minierp.backend.domain.project.dto.ProjectResponseDto;
import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.project.entity.ProjectMember;
import com.minierp.backend.domain.project.repository.ProjectMemberRepository;
import com.minierp.backend.domain.project.repository.ProjectRepository;
import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.task.entity.TaskStatus;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponseDto createProject(ProjectCreateRequestDto request, UserRole currentUserRole) {
        validateAdminRole(currentUserRole);

        User leader = null;
        if (request.getLeaderId() != null) {
            leader = findUserOrThrow(request.getLeaderId());
            if (leader.getUserRole() != UserRole.TEAM_LEADER) {
                throw new BusinessException(ErrorCode.INVALID_LEADER_ROLE);
            }
        }

        Project project = Project.create(
                request.getTitle(),
                request.getContent(),
                request.getStartDate(),
                request.getEndDate(),
                leader
        );

        Project savedProject = projectRepository.save(project);
        return ProjectResponseDto.from(savedProject);
    }

    public List<ProjectResponseDto> getProjects(Long currentUserId, UserRole currentUserRole) {
        if (currentUserRole == UserRole.ADMIN) {
            return projectRepository.findAll().stream()
                    .map(this::toProjectResponseDto)
                    .toList();
        }

        validateCurrentUserId(currentUserId);

        if (currentUserRole == UserRole.TEAM_LEADER) {
            List<Project> leaderProjects = projectRepository.findByLeaderId(currentUserId);
            if (leaderProjects.isEmpty()) {
                throw new BusinessException(ErrorCode.NO_ASSIGNED_PROJECT);
            }
            return leaderProjects.stream()
                    .map(this::toProjectResponseDto)
                    .toList();
        }

        return projectMemberRepository.findByUserId(currentUserId).stream()
                .map(ProjectMember::getProject)
                .distinct()
                .map(this::toProjectResponseDto)
                .toList();
    }

    @Transactional
    public ProjectMemberResponseDto addMember(
            Long projectId,
            Long userId,
            Long currentUserId,
            UserRole currentUserRole
    ) {
        validateAdminOrLeaderRole(currentUserRole);
        validateProjectLeader(projectId, currentUserId, currentUserRole);

        Project project = findProjectOrThrow(projectId);
        User user = findUserOrThrow(userId);

        if (currentUserRole == UserRole.TEAM_LEADER && user.getUserRole() != UserRole.USER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "팀장은 팀원(USER)만 프로젝트에 배정할 수 있습니다.");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PROJECT_MEMBER);
        }

        ProjectMember projectMember = ProjectMember.create(project, user);
        ProjectMember savedProjectMember = projectMemberRepository.save(projectMember);
        return ProjectMemberResponseDto.from(savedProjectMember);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, Long currentUserId, UserRole currentUserRole) {
        validateAdminOrLeaderRole(currentUserRole);
        validateProjectLeader(projectId, currentUserId, currentUserRole);

        findProjectOrThrow(projectId);
        User user = findUserOrThrow(userId);

        if (currentUserRole == UserRole.TEAM_LEADER && user.getUserRole() != UserRole.USER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "팀장은 팀원(USER)만 프로젝트에서 제거할 수 있습니다.");
        }

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND);
        }

        taskAssignmentRepository.deleteByProjectIdAndUserId(projectId, userId);
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    public List<ProjectMemberResponseDto> getMembers(Long projectId, Long currentUserId, UserRole currentUserRole) {
        validateAdminOrLeaderRole(currentUserRole);
        validateProjectLeader(projectId, currentUserId, currentUserRole);

        findProjectOrThrow(projectId);

        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(ProjectMemberResponseDto::from)
                .toList();
    }

    public List<ProjectMemberResponseDto> getAvailableMembers(Long projectId, Long currentUserId, UserRole currentUserRole) {
        validateAdminOrLeaderRole(currentUserRole);
        validateProjectLeader(projectId, currentUserId, currentUserRole);

        findProjectOrThrow(projectId);

        List<User> users = userRepository.findByUserRole(UserRole.USER);
        List<Long> assignedUserIds = projectMemberRepository.findByProjectId(projectId).stream()
                .map(projectMember -> projectMember.getUser().getId())
                .toList();

        return users.stream()
                .filter(user -> !assignedUserIds.contains(user.getId()))
                .map(user -> ProjectMemberResponseDto.fromUser(projectId, user.getId(), user.getUserName()))
                .toList();
    }

    public ProjectProgressResponseDto getProjectProgress(Long projectId, Long currentUserId, UserRole currentUserRole) {
        findProjectOrThrow(projectId);
        validateProjectAccess(projectId, currentUserId, currentUserRole);

        List<Task> tasks = taskRepository.findByProjectId(projectId);
        long totalTasks = tasks.size();
        long doneTasks = tasks.stream()
                .filter(task -> task.getTaskStatus() == TaskStatus.DONE)
                .count();
        int progressRate = totalTasks == 0 ? 0 : (int) ((doneTasks * 100) / totalTasks);

        return ProjectProgressResponseDto.of(projectId, totalTasks, doneTasks, progressRate);
    }

    private ProjectResponseDto toProjectResponseDto(Project project) {
        List<Task> tasks = taskRepository.findByProjectId(project.getId());
        long totalTasks = tasks.size();
        long doneTasks = tasks.stream()
                .filter(task -> task.getTaskStatus() == TaskStatus.DONE)
                .count();
        int progressRate = totalTasks == 0 ? 0 : (int) ((doneTasks * 100) / totalTasks);
        long memberCount = projectMemberRepository.findByProjectId(project.getId()).size();

        return ProjectResponseDto.from(project, memberCount, totalTasks, progressRate);
    }

    private void validateAdminRole(UserRole currentUserRole) {
        if (currentUserRole != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateAdminOrLeaderRole(UserRole currentUserRole) {
        if (currentUserRole != UserRole.ADMIN && currentUserRole != UserRole.TEAM_LEADER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateCurrentUserId(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "현재 사용자 정보가 필요합니다.");
        }
    }

    private void validateProjectLeader(Long projectId, Long currentUserId, UserRole currentUserRole) {
        if (currentUserRole == UserRole.ADMIN) {
            return;
        }

        if (currentUserRole != UserRole.TEAM_LEADER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        validateCurrentUserId(currentUserId);
        Project project = findProjectOrThrow(projectId);
        if (project.getLeader() == null || !project.getLeader().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 프로젝트의 담당 팀장이 아닙니다.");
        }
    }

    private void validateProjectAccess(Long projectId, Long currentUserId, UserRole currentUserRole) {
        if (currentUserRole == UserRole.ADMIN) {
            return;
        }

        validateCurrentUserId(currentUserId);

        if (currentUserRole == UserRole.TEAM_LEADER) {
            Project project = findProjectOrThrow(projectId);
            if (project.getLeader() != null && project.getLeader().getId().equals(currentUserId)) {
                return;
            }
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
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
