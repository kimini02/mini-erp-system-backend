package com.minierp.backend.domain.project.service;

import com.minierp.backend.domain.project.dto.ProjectCreateRequestDto;
import com.minierp.backend.domain.project.dto.ProjectPermissionDto;
import com.minierp.backend.domain.project.dto.ProjectPermissionUpdateRequestDto;
import com.minierp.backend.domain.project.dto.ProjectMemberResponseDto;
import com.minierp.backend.domain.project.dto.ProjectProgressResponseDto;
import com.minierp.backend.domain.project.dto.ProjectResponseDto;
import com.minierp.backend.domain.project.dto.ProjectUpdateRequestDto;
import com.minierp.backend.domain.project.dto.AvailableMemberResponseDto;
import com.minierp.backend.domain.project.dto.AssignableMemberDto;
import com.minierp.backend.domain.project.dto.LeaderSummaryDto;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 프로젝트 서비스
 * - 프로젝트 CRUD 및 멤버 배정/해제
 * - 역할별 접근 제어: ADMIN(전체), TEAM_LEADER(담당 프로젝트만), USER(배정된 프로젝트만)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // 프로젝트 생성: ADMIN만 가능, 담당 팀장은 TEAM_LEADER 역할만 배정 가능
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
                request.getPriority(),
                leader
        );

        Project savedProject = projectRepository.save(project);
        return ProjectResponseDto.from(savedProject);
    }

    // 프로젝트 목록 조회: ADMIN=전체, TEAM_LEADER=담당 프로젝트, USER=배정된 프로젝트
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
    public ProjectResponseDto updateProject(Long projectId, ProjectUpdateRequestDto request, UserRole currentUserRole) {
        validateAdminRole(currentUserRole);

        Project project = findProjectOrThrow(projectId);
        project.update(
                request.getTitle(),
                request.getContent(),
                request.getStartDate(),
                request.getEndDate(),
                request.getPriority()
        );

        return toProjectResponseDto(project);
    }

    @Transactional
    public ProjectResponseDto updateProjectLeader(Long projectId, Long leaderId, UserRole currentUserRole) {
        validateAdminRole(currentUserRole);

        Project project = findProjectOrThrow(projectId);
        User leader = findUserOrThrow(leaderId);

        if (leader.getUserRole() != UserRole.TEAM_LEADER) {
            throw new BusinessException(ErrorCode.INVALID_LEADER_ROLE);
        }

        project.assignLeader(leader);
        return toProjectResponseDto(project);
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

    // 멤버 제거 시 해당 유저의 Task 배정(TaskAssignment)도 함께 삭제
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

    // 배정 가능 멤버 조회: 전체 USER 중 해당 프로젝트에 아직 배정되지 않은 유저만 반환
    public List<AvailableMemberResponseDto> getAvailableMembers(Long projectId, Long currentUserId, UserRole currentUserRole) {
        validateAdminOrLeaderRole(currentUserRole);
        validateProjectLeader(projectId, currentUserId, currentUserRole);

        findProjectOrThrow(projectId);

        List<User> users = userRepository.findByUserRole(UserRole.USER);
        List<Long> assignedUserIds = projectMemberRepository.findByProjectId(projectId).stream()
                .map(projectMember -> projectMember.getUser().getId())
                .toList();

        return users.stream()
                .filter(user -> !assignedUserIds.contains(user.getId()))
                .map(AvailableMemberResponseDto::from)
                .toList();
    }

    // Task에 배정 가능한 멤버 조회: 이미 프로젝트에 배정된 USER 역할 멤버만 반환
    public List<AssignableMemberDto> getAssignableMembers(Long projectId, Long currentUserId, UserRole currentUserRole) {
        validateAdminOrLeaderRole(currentUserRole);
        if (currentUserRole.isTeamLeader()) {
            validateProjectLeader(projectId, currentUserId, currentUserRole);
        }

        findProjectOrThrow(projectId);

        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(ProjectMember::getUser)
                .filter(user -> user.getUserRole().isGeneralUser())
                .map(AssignableMemberDto::from)
                .toList();
    }

    public List<ProjectPermissionDto> getUserProjectPermissions(
            Long targetUserId,
            Long currentUserId,
            UserRole currentUserRole
    ) {
        validateAdminOrLeaderRole(currentUserRole);

        List<Project> projects = currentUserRole.isTopManager()
                ? projectRepository.findAll()
                : projectRepository.findByLeaderId(currentUserId);

        Set<Long> assignedProjectIds = projectMemberRepository.findByUserId(targetUserId).stream()
                .map(projectMember -> projectMember.getProject().getId())
                .collect(java.util.stream.Collectors.toSet());

        return projects.stream()
                .map(project -> ProjectPermissionDto.of(project, assignedProjectIds.contains(project.getId())))
                .toList();
    }

    @Transactional
    public void updateUserProjectPermissions(
            Long targetUserId,
            ProjectPermissionUpdateRequestDto request,
            Long currentUserId,
            UserRole currentUserRole
    ) {
        validateAdminOrLeaderRole(currentUserRole);

        User targetUser = findUserOrThrow(targetUserId);
        if (currentUserRole.isTeamLeader() && !targetUser.getUserRole().isGeneralUser()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "팀장은 팀원(USER)의 권한만 설정할 수 있습니다.");
        }

        List<Project> manageableProjects = currentUserRole.isTopManager()
                ? projectRepository.findAll()
                : projectRepository.findByLeaderId(currentUserId);
        Set<Long> manageableProjectIds = manageableProjects.stream()
                .map(Project::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> requestedProjectIds = new HashSet<>(request.getAssignedProjectIds());

        for (Long projectId : requestedProjectIds) {
            if (!manageableProjectIds.contains(projectId)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "관리 권한이 없는 프로젝트입니다.");
            }
        }

        Set<Long> currentAssignedIds = projectMemberRepository.findByUserId(targetUserId).stream()
                .map(projectMember -> projectMember.getProject().getId())
                .filter(manageableProjectIds::contains)
                .collect(java.util.stream.Collectors.toSet());

        Set<Long> toAdd = new HashSet<>(requestedProjectIds);
        toAdd.removeAll(currentAssignedIds);

        Set<Long> toRemove = new HashSet<>(currentAssignedIds);
        toRemove.removeAll(requestedProjectIds);

        for (Long projectId : toAdd) {
            Project project = findProjectOrThrow(projectId);
            ProjectMember projectMember = ProjectMember.create(project, targetUser);
            projectMemberRepository.save(projectMember);
        }

        for (Long projectId : toRemove) {
            taskAssignmentRepository.deleteByProjectIdAndUserId(projectId, targetUserId);
            projectMemberRepository.deleteByProjectIdAndUserId(projectId, targetUserId);
        }
    }

    public List<LeaderSummaryDto> getLeaders(UserRole currentUserRole) {
        validateAdminRole(currentUserRole);

        return userRepository.findByUserRole(UserRole.TEAM_LEADER).stream()
                .map(leader -> LeaderSummaryDto.of(leader, projectRepository.countByLeaderId(leader.getId())))
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
