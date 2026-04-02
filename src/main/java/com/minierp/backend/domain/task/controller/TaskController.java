package com.minierp.backend.domain.task.controller;

import com.minierp.backend.domain.task.dto.TaskAssignmentRequestDto;
import com.minierp.backend.domain.task.dto.TaskAssignmentResponseDto;
import com.minierp.backend.domain.task.dto.TaskCreateRequestDto;
import com.minierp.backend.domain.task.dto.RecentAssignmentDto;
import com.minierp.backend.domain.task.dto.TaskResponseDto;
import com.minierp.backend.domain.task.dto.TaskStatusUpdateDto;
import com.minierp.backend.domain.task.dto.TaskUpdateRequestDto;
import com.minierp.backend.domain.task.service.TaskService;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import com.minierp.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponseDto>> createTask(
            @Valid @RequestBody TaskCreateRequestDto request,
            Authentication authentication
    ) {
        TaskResponseDto response = taskService.createTask(
                request,
                extractUserId(authentication),
                extractUserRole(authentication)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Task가 생성되었습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> getTasks(Authentication authentication) {
        List<TaskResponseDto> response = taskService.getTasks(
                extractUserId(authentication),
                extractUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "업무 목록 조회가 완료되었습니다."));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponseDto>> getTask(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        TaskResponseDto response = taskService.getTask(
                taskId,
                extractUserId(authentication),
                extractUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "업무 상세 조회가 완료되었습니다."));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponseDto>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequestDto request,
            Authentication authentication
    ) {
        TaskResponseDto response = taskService.updateTask(
                taskId,
                request,
                extractUserId(authentication),
                extractUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "업무가 수정되었습니다."));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<TaskResponseDto>> changeTaskStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusUpdateDto request,
            Authentication authentication
    ) {
        TaskResponseDto response = taskService.changeTaskStatus(
                taskId,
                extractUserId(authentication),
                extractUserRole(authentication),
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Task 상태가 변경되었습니다."));
    }

    @PostMapping("/{taskId}/assignments")
    public ResponseEntity<ApiResponse<TaskAssignmentResponseDto>> addAssignment(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskAssignmentRequestDto request,
            Authentication authentication
    ) {
        TaskAssignmentResponseDto response = taskService.addAssignment(
                taskId,
                request.getUserId(),
                extractUserId(authentication),
                extractUserRole(authentication)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "업무 담당자가 배정되었습니다."));
    }

    @GetMapping("/{taskId}/assignments")
    public ResponseEntity<ApiResponse<List<TaskAssignmentResponseDto>>> getAssignments(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        List<TaskAssignmentResponseDto> response = taskService.getAssignments(
                taskId,
                extractUserId(authentication),
                extractUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "업무 담당자 목록 조회가 완료되었습니다."));
    }

    @DeleteMapping("/{taskId}/assignments/{userId}")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable Long taskId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        taskService.removeAssignment(taskId, userId, extractUserId(authentication), extractUserRole(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recent-assignments")
    public ResponseEntity<ApiResponse<List<RecentAssignmentDto>>> getRecentAssignments(Authentication authentication) {
        List<RecentAssignmentDto> response = taskService.getRecentAssignments(
                extractUserId(authentication),
                extractUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "최근 업무 배정 이력 조회가 완료되었습니다."));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증 사용자 정보가 올바르지 않습니다.");
        }
    }

    private UserRole extractUserRole(Authentication authentication) {
        if (authentication == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.replace("ROLE_", ""))
                .map(UserRole::valueOf)
                .findFirst()
                .orElse(UserRole.USER);
    }
}
