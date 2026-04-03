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
import com.minierp.backend.domain.task.entity.TaskStatus;
import com.minierp.backend.domain.task.repository.TaskRepository;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import com.minierp.backend.global.service.AccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.minierp.backend.domain.approval.dto.LeaveRequestResponseDto;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

/**
 * 대시보드 서비스
 * - Task 상태별 통계 (TODO/DOING/DONE 개수 + 진행률)
 * - 관리자 요약: ADMIN=전체 통계, TEAM_LEADER=담당 프로젝트 기준 통계
 * - 대시보드 프로젝트 목록: 진행 중(PROGRESS) 우선, 마감일순 상위 5개
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AccessPolicy accessPolicy;

    public DashboardResponseDto getDashboardStats(Long currentUserId, UserRole currentUserRole) {
        List<Task> tasks;

        if (currentUserRole == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (currentUserRole.isTopManager()) {
            tasks = taskRepository.findAll();
        } else if (currentUserRole.isTeamLeader()) {
            List<Long> projectIds = projectRepository.findByLeaderId(currentUserId).stream()
                    .map(Project::getId)
                    .toList();
            tasks = projectIds.stream()
                    .flatMap(projectId -> taskRepository.findByProjectId(projectId).stream())
                    .toList();
        } else {
            tasks = taskRepository.findByAssigneeUserId(currentUserId);
        }

        Map<TaskStatus, Long> taskStatusStats = tasks.stream()
                .collect(groupingBy(
                        Task::getTaskStatus,
                        () -> new EnumMap<>(TaskStatus.class),
                        counting()
                ));

        return DashboardResponseDto.of(taskStatusStats);
    }

    // 관리자 요약: ADMIN=전체 사용자/프로젝트/Task 통계, TEAM_LEADER=담당 프로젝트 기준
    public AdminDashboardResponseDto getAdminSummary(Long currentUserId, UserRole currentUserRole) {
        accessPolicy.requireAdminOrLeader(currentUserRole);

        long totalUsers = 0L;
        long activeProjectCount;
        long totalTaskCount;
        long doneTaskCount;

        if (currentUserRole.isTopManager()) {
            totalUsers = userRepository.count();
            activeProjectCount = projectRepository.countByStatus(ProjectStatus.PROGRESS);
            totalTaskCount = taskRepository.count();
            doneTaskCount = taskRepository.countByTaskStatus(TaskStatus.DONE);
        } else {
            List<Project> leaderProjects = projectRepository.findByLeaderId(currentUserId);
            List<Long> projectIds = leaderProjects.stream()
                    .map(Project::getId)
                    .toList();

            activeProjectCount = leaderProjects.stream()
                    .filter(project -> project.getStatus() == ProjectStatus.PROGRESS)
                    .count();
            totalTaskCount = projectIds.isEmpty() ? 0L : taskRepository.countByProjectIdIn(projectIds);
            doneTaskCount = projectIds.isEmpty() ? 0L : taskRepository.countByProjectIdInAndTaskStatus(projectIds, TaskStatus.DONE);
        }

        double taskCompletionRate = totalTaskCount == 0 ? 0.0 : (doneTaskCount * 100.0) / totalTaskCount;
        long pendingApprovalCount = leaveRequestRepository.countByAppStatus(LeaveStatus.PENDING);

        List<LeaveRequestResponseDto> pendingApprovals = leaveRequestRepository.findAll().stream()
                .filter(req -> req.getAppStatus() == LeaveStatus.PENDING)
                .map(LeaveRequestResponseDto::from)
                .toList();

        return AdminDashboardResponseDto.of(
                totalUsers,
                activeProjectCount,
                pendingApprovalCount,
                taskCompletionRate,
                totalTaskCount,
                pendingApprovals
        );
    }

    // 대시보드 프로젝트 카드: 진행 중 우선 → 마감일순 정렬, 상위 5개 + 진척률 포함
    public List<DashboardProjectDto> getDashboardProjects(Long currentUserId, UserRole currentUserRole) {
        accessPolicy.requireAdminOrLeader(currentUserRole);

        List<Project> projects = currentUserRole.isTopManager()
                ? projectRepository.findAll()
                : projectRepository.findByLeaderId(currentUserId);

        return projects.stream()
                .sorted(Comparator.comparing((Project project) -> project.getStatus() == ProjectStatus.PROGRESS ? 0 : 1)
                        .thenComparing(Project::getEndDate))
                .limit(5)
                .map(project -> {
                    long totalTaskCount = taskRepository.countByProjectId(project.getId());
                    long doneTaskCount = taskRepository.countByProjectIdAndTaskStatus(project.getId(), TaskStatus.DONE);
                    int progressRate = totalTaskCount == 0 ? 0 : (int) ((doneTaskCount * 100) / totalTaskCount);
                    return DashboardProjectDto.from(project, progressRate);
                })
                .toList();
    }
}