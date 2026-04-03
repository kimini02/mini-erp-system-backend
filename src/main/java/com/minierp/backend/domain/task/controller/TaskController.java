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
import com.minierp.backend.global.security.CurrentUserResolver;
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
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponseDto>> createTask(
            @Valid @RequestBody TaskCreateRequestDto request,
            Authentication authentication
    ) {
        TaskResponseDto response = taskService.createTask(
                request,
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Task가 생성되었습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> getTasks(Authentication authentication) {
        List<TaskResponseDto> response = taskService.getTasks(
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
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
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
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
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
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
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication),
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
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
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
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "업무 담당자 목록 조회가 완료되었습니다."));
    }

    @DeleteMapping("/{taskId}/assignments/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeAssignment(
            @PathVariable Long taskId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        taskService.removeAssignment(
                taskId,
                userId,
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.successMessage("업무 담당자 배정이 해제되었습니다."));
    }

    @GetMapping("/recent-assignments")
    public ResponseEntity<ApiResponse<List<RecentAssignmentDto>>> getRecentAssignments(Authentication authentication) {
        List<RecentAssignmentDto> response = taskService.getRecentAssignments(
                currentUserResolver.resolveUserId(authentication),
                currentUserResolver.resolveUserRole(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response, "최근 업무 배정 이력 조회가 완료되었습니다."));
    }
}
