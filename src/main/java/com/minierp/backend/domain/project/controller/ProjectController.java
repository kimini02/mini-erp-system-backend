package com.minierp.backend.domain.project.controller;

import com.minierp.backend.domain.project.dto.AssignableMemberDto;
import com.minierp.backend.domain.project.dto.LeaderSummaryDto;
import com.minierp.backend.domain.project.dto.MemberRequestDto;
import com.minierp.backend.domain.project.dto.ProjectCreateRequestDto;
import com.minierp.backend.domain.project.dto.ProjectLeaderUpdateRequestDto;
import com.minierp.backend.domain.project.dto.ProjectPermissionDto;
import com.minierp.backend.domain.project.dto.ProjectPermissionUpdateRequestDto;
import com.minierp.backend.domain.project.dto.AvailableMemberResponseDto;
import com.minierp.backend.domain.project.dto.ProjectMemberResponseDto;
import com.minierp.backend.domain.project.dto.ProjectProgressResponseDto;
import com.minierp.backend.domain.project.dto.ProjectResponseDto;
import com.minierp.backend.domain.project.dto.ProjectUpdateRequestDto;
import com.minierp.backend.domain.project.service.ProjectService;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import com.minierp.backend.global.response.ApiResponse;
import com.minierp.backend.global.security.CurrentUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponseDto>> createProject(
            @Valid @RequestBody ProjectCreateRequestDto request,
            Authentication authentication
    ) {
        ProjectResponseDto response = projectService.createProject(request, currentUserResolver.resolveUserRole(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "프로젝트가 생성되었습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponseDto>>> getProjects(Authentication authentication) {
        List<ProjectResponseDto> response = projectService.getProjects(
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "프로젝트 목록 조회가 완료되었습니다."));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponseDto>> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequestDto request,
            Authentication authentication
    ) {
        ProjectResponseDto response = projectService.updateProject(
                projectId,
                request,
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "프로젝트가 수정되었습니다."));
    }

    @PatchMapping("/{projectId}/leader")
    public ResponseEntity<ApiResponse<ProjectResponseDto>> updateProjectLeader(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectLeaderUpdateRequestDto request,
            Authentication authentication
    ) {
        ProjectResponseDto response = projectService.updateProjectLeader(
                projectId,
                request.getLeaderId(),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "프로젝트 팀장이 변경되었습니다."));
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<ProjectMemberResponseDto>> addMember(
            @PathVariable Long projectId,
            @Valid @RequestBody MemberRequestDto request,
            Authentication authentication
    ) {
        ProjectMemberResponseDto response = projectService.addMember(
                projectId,
                request.getUserId(),
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "프로젝트 팀원이 배정되었습니다."));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        projectService.removeMember(
                projectId,
                userId,
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.successMessage("프로젝트 팀원 배정이 해제되었습니다."));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponseDto>>> getMembers(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        List<ProjectMemberResponseDto> response = projectService.getMembers(
                projectId,
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "프로젝트 팀원 목록 조회가 완료되었습니다."));
    }

    // 프로젝트에 배정되지 않은 팀원
    @GetMapping("/{projectId}/members/available")
    public ResponseEntity<ApiResponse<List<AvailableMemberResponseDto>>> getAvailableMembers(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        List<AvailableMemberResponseDto> response = projectService.getAvailableMembers(
                projectId,
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "배정 가능한 팀원 목록 조회가 완료되었습니다."));
    }

    // 프로젝트에 이미 배정된 팀원 중, Task에 배정 가능한 사람
    @GetMapping("/{projectId}/members/assignable")
    public ResponseEntity<ApiResponse<List<AssignableMemberDto>>> getAssignableMembers(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        List<AssignableMemberDto> response = projectService.getAssignableMembers(
                projectId,
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "배정 가능한 팀원 목록 조회가 완료되었습니다."));
    }

    @GetMapping("/permissions/{userId}")
    public ResponseEntity<ApiResponse<List<ProjectPermissionDto>>> getUserProjectPermissions(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        List<ProjectPermissionDto> response = projectService.getUserProjectPermissions(
                userId,
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "사용자 프로젝트 권한 조회가 완료되었습니다."));
    }

    @PutMapping("/permissions/{userId}")
    public ResponseEntity<ApiResponse<Void>> updateUserProjectPermissions(
            @PathVariable Long userId,
            @Valid @RequestBody ProjectPermissionUpdateRequestDto request,
            Authentication authentication
    ) {
        projectService.updateUserProjectPermissions(
                userId,
                request,
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(null, "프로젝트 권한이 저장되었습니다."));
    }

    @GetMapping("/leaders")
    public ResponseEntity<ApiResponse<List<LeaderSummaryDto>>> getLeaders(Authentication authentication) {
        List<LeaderSummaryDto> response = projectService.getLeaders(currentUserResolver.resolveUserRole(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "팀장 목록 조회가 완료되었습니다."));
    }

    @GetMapping("/{projectId}/progress")
    public ResponseEntity<ApiResponse<ProjectProgressResponseDto>> getProjectProgress(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        ProjectProgressResponseDto response = projectService.getProjectProgress(
                projectId,
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "프로젝트 진행률 조회가 완료되었습니다."));
    }
}
