package com.minierp.backend.domain.user.controller;

import com.minierp.backend.domain.user.dto.FindIdRequestDto;
import com.minierp.backend.domain.user.dto.FindIdResponseDto;
import com.minierp.backend.domain.user.dto.LoginRequestDto;
import com.minierp.backend.domain.user.dto.LoginResponseDto;
import com.minierp.backend.domain.user.dto.PasswordResetConfirmDto;
import com.minierp.backend.domain.user.dto.PasswordResetConfirmResponseDto;
import com.minierp.backend.domain.user.dto.PasswordResetRequestDto;
import com.minierp.backend.domain.user.dto.PasswordResetRequestResponseDto;
import com.minierp.backend.domain.user.dto.PasswordResetVerifyDto;
import com.minierp.backend.domain.user.dto.PasswordResetVerifyResponseDto;
import com.minierp.backend.domain.user.dto.SignupRequestDto;
import com.minierp.backend.domain.user.dto.UserResponseDto;
import com.minierp.backend.domain.user.service.AuthService;
import com.minierp.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponseDto>> signup(@Valid @RequestBody SignupRequestDto requestDto) {
        UserResponseDto response = authService.signup(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto requestDto) {
        LoginResponseDto response = authService.login(requestDto);
        return ResponseEntity.ok(ApiResponse.success(response, "로그인이 성공하였습니다"));
    }

    @PostMapping("/find-id/request")
    public ResponseEntity<ApiResponse<FindIdResponseDto>> findId(@Valid @RequestBody FindIdRequestDto requestDto) {
        FindIdResponseDto response = authService.findId(requestDto);
        return ResponseEntity.ok(ApiResponse.success(response, "아이디를 찾았습니다"));
    }

    @PostMapping("/password/reset/request")
    public ResponseEntity<ApiResponse<PasswordResetRequestResponseDto>> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto requestDto) {
        PasswordResetRequestResponseDto response = authService.requestPasswordReset(requestDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/password/reset/verify")
    public ResponseEntity<ApiResponse<PasswordResetVerifyResponseDto>> verifyPasswordReset(@Valid @RequestBody PasswordResetVerifyDto requestDto) {
        PasswordResetVerifyResponseDto response = authService.verifyPasswordReset(requestDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/password/reset/confirm")
    public ResponseEntity<ApiResponse<PasswordResetConfirmResponseDto>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDto requestDto) {
        PasswordResetConfirmResponseDto response = authService.confirmPasswordReset(requestDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
