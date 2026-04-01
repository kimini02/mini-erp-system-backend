package com.minierp.backend.domain.user.service;

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
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import com.minierp.backend.global.security.JwtTokenProvider;
import com.minierp.backend.global.service.MailService;
import com.minierp.backend.global.service.PasswordResetStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final PasswordResetStoreService passwordResetStoreService;

    @Transactional
    public UserResponseDto signup(SignupRequestDto requestDto) {
        if (userRepository.existsByLoginId(requestDto.getId())) {
            throw new BusinessException(ErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }
        if (userRepository.existsByUserEmail(requestDto.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
        User user = User.create(
                requestDto.getName(),
                requestDto.getId(),
                requestDto.getEmail(),
                encodedPassword,
                requestDto.getPosition()
        );

        User savedUser = userRepository.save(user);
        return UserResponseDto.from(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto requestDto) {
        User user = userRepository.findByLoginId(requestDto.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getUserPw())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getLoginId(), user.getUserRole().name());
        return LoginResponseDto.of(
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                UserResponseDto.from(user)
        );
    }

    @Transactional(readOnly = true)
    public FindIdResponseDto findId(FindIdRequestDto requestDto) {
        User user = userRepository.findByUserEmail(requestDto.getEmail())
                .filter(u -> u.getUserName().equals(requestDto.getName()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당하는 사용자를 찾을 수 없습니다."));

        return FindIdResponseDto.of(user.getLoginId());
    }

    @Transactional
    public PasswordResetRequestResponseDto requestPasswordReset(PasswordResetRequestDto requestDto) {
        long requestCount = passwordResetStoreService.incrementRequestCount(requestDto.getEmail());
        if (requestCount > passwordResetStoreService.getMaxRequestsPerMinute()) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        userRepository.findByUserEmail(requestDto.getEmail()).ifPresent(user -> {
            String verificationCode = generateVerificationCode();
            passwordResetStoreService.saveVerificationCode(user.getUserEmail(), verificationCode);
            mailService.sendVerificationCodeMail(user.getUserEmail(), verificationCode);
        });

        // 계정 존재 여부와 관계없이 동일 응답
        return PasswordResetRequestResponseDto.success();
    }

    @Transactional
    public PasswordResetVerifyResponseDto verifyPasswordReset(PasswordResetVerifyDto requestDto) {
        User user = userRepository.findByUserEmail(requestDto.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESET_REQUEST));

        String savedCode = passwordResetStoreService.getVerificationCode(user.getUserEmail());
        if (savedCode == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        if (!savedCode.equals(requestDto.getVerificationCode())) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        passwordResetStoreService.removeVerificationCode(user.getUserEmail());

        String resetProof = UUID.randomUUID().toString();
        passwordResetStoreService.saveResetProof(resetProof, user.getUserEmail());

        return PasswordResetVerifyResponseDto.of(resetProof);
    }

    @Transactional
    public PasswordResetConfirmResponseDto confirmPasswordReset(PasswordResetConfirmDto requestDto) {
        if (!requestDto.getNewPassword().equals(requestDto.getNewPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "비밀번호 확인이 일치하지 않습니다.");
        }

        validatePasswordPolicy(requestDto.getNewPassword());

        String email = passwordResetStoreService.getEmailByResetProof(requestDto.getResetProof());
        if (email == null) {
            throw new BusinessException(ErrorCode.RESET_PROOF_INVALID);
        }

        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESET_REQUEST));

        // 기존 비밀번호와 동일한지 확인 (BCrypt)
        if (passwordEncoder.matches(requestDto.getNewPassword(), user.getUserPw())) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "이전과 다른 비밀번호를 입력해주세요.");
        }

        String encodedPassword = passwordEncoder.encode(requestDto.getNewPassword());
        user.updatePassword(encodedPassword);
        passwordResetStoreService.removeResetProof(requestDto.getResetProof());

        return PasswordResetConfirmResponseDto.success();
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void validatePasswordPolicy(String password) {
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        if (!hasUpperCase || !hasLowerCase || !hasDigit || !hasSpecialChar) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION,
                    "비밀번호는 대문자, 소문자, 숫자, 특수문자를 모두 포함해야 합니다.");
        }
    }
}
