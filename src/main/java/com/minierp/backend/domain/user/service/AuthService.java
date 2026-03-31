package com.minierp.backend.domain.user.service;

import com.minierp.backend.domain.user.dto.LoginRequestDto;
import com.minierp.backend.domain.user.dto.LoginResponseDto;
import com.minierp.backend.domain.user.dto.SignupRequestDto;
import com.minierp.backend.domain.user.dto.UserResponseDto;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import com.minierp.backend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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
}
