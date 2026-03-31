package com.minierp.backend.domain.user.controller;

import com.minierp.backend.domain.user.dto.UserListResponseDto;
import com.minierp.backend.domain.user.dto.UserResponseDto;
import com.minierp.backend.domain.user.dto.UserRoleUpdateRequestDto;
import com.minierp.backend.domain.user.dto.UserRoleUpdateResponseDto;
import com.minierp.backend.domain.user.dto.UserUpdateRequestDto;
import com.minierp.backend.domain.user.service.UserService;
import com.minierp.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserListResponseDto>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search
    ) {
        UserListResponseDto response = userService.getUsers(page, size, role, search);
        return ResponseEntity.ok(ApiResponse.success(response, "목록 조회가 완료되었습니다"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUser(@PathVariable Long userId, Authentication authentication) {
        UserResponseDto response = userService.getUser(userId, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequestDto requestDto,
            Authentication authentication
    ) {
        UserResponseDto response = userService.updateUser(userId, requestDto, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "사용자 정보가 수정되었습니다"));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserRoleUpdateResponseDto>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequestDto requestDto
    ) {
        UserRoleUpdateResponseDto response = userService.updateUserRole(userId, requestDto.getRole());
        return ResponseEntity.ok(ApiResponse.success(response, "사용자 권한이 변경되었습니다"));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
