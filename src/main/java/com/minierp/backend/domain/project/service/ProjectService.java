package com.minierp.backend.domain.project.service;

import com.minierp.backend.domain.project.dto.ProjectCreateRequestDto;
import com.minierp.backend.domain.project.dto.ProjectMemberResponseDto;
import com.minierp.backend.domain.project.dto.ProjectProgressResponseDto;
import com.minierp.backend.domain.project.dto.ProjectResponseDto;
import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.project.entity.ProjectMember;
import com.minierp.backend.domain.project.repository.ProjectMemberRepository;
import com.minierp.backend.domain.project.repository.ProjectRepository;
import com.minierp.backend.domain.task.entity.TaskStatus;
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
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponseDto createProject(ProjectCreateRequestDto request, UserRole currentUserRole) {
        validateAdminRole(currentUserRole);

        Project project = Project.create(
                request.getTitle(),
                request.getContent(),
                request.getStartDate(),
                request.getEndDate()
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

        return projectMemberRepository.findByUserId(currentUserId).stream()
                .map(ProjectMember::getProject)
                .distinct()
                .map(this::toProjectResponseDto)
                .toList();
    }

    @Transactional
    public ProjectMemberResponseDto addMember(Long projectId, Long userId, UserRole currentUserRole) {
        validateAdminRole(currentUserRole);

        Project project = findProjectOrThrow(projectId);
        User user = findUserOrThrow(userId);

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PROJECT_MEMBER);
        }

        ProjectMember projectMember = ProjectMember.create(project, user);
        ProjectMember savedProjectMember = projectMemberRepository.save(projectMember);
        return ProjectMemberResponseDto.from(savedProjectMember);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, UserRole currentUserRole) {
        validateAdminRole(currentUserRole);

        findProjectOrThrow(projectId);
        findUserOrThrow(userId);

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND);
        }

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    public List<ProjectMemberResponseDto> getMembers(Long projectId, UserRole currentUserRole) {
        validateAdminRole(currentUserRole);
        findProjectOrThrow(projectId);

        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(ProjectMemberResponseDto::from)
                .toList();
    }

    public ProjectProgressResponseDto getProjectProgress(Long projectId, Long currentUserId, UserRole currentUserRole) {
        findProjectOrThrow(projectId);
        validateProjectAccess(projectId, currentUserId, currentUserRole);

        long totalTasks = taskRepository.countByProjectId(projectId);
        long doneTasks = taskRepository.countByProjectIdAndTaskStatus(projectId, TaskStatus.DONE);
        int progressRate = totalTasks == 0 ? 0 : (int) ((doneTasks * 100) / totalTasks);

        return ProjectProgressResponseDto.of(projectId, totalTasks, doneTasks, progressRate);
    }

    private ProjectResponseDto toProjectResponseDto(Project project) {
        long memberCount = projectMemberRepository.countByProjectId(project.getId());
        long taskCount = taskRepository.countByProjectId(project.getId());
        long doneCount = taskRepository.countByProjectIdAndTaskStatus(project.getId(), TaskStatus.DONE);
        int progressRate = taskCount == 0 ? 0 : (int) ((doneCount * 100) / taskCount);

        return ProjectResponseDto.from(project, memberCount, taskCount, progressRate);
    }

    private void validateAdminRole(UserRole currentUserRole) {
        if (currentUserRole != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateCurrentUserId(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "현재 사용자 정보가 필요합니다.");
        }
    }

    private void validateProjectAccess(Long projectId, Long currentUserId, UserRole currentUserRole) {
        if (currentUserRole == UserRole.ADMIN) {
            return;
        }

        validateCurrentUserId(currentUserId);

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
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
