package com.minierp.backend.domain.user.controller;

import com.minierp.backend.domain.user.dto.SignupRequestDto;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    // ⭐️ 회원가입 API (스웨거에 POST 버튼을 만들어 줍니다!)
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequestDto dto) {
        // 입력받은 정보로 가짜 유저(연차 15일 기본 제공) 생성
        User user = User.builder()
                .userName(dto.getUserName())
                .userEmail(dto.getUserEmail())
                .userPw(dto.getUserPw())
                .positionName(dto.getPositionName())
                .userRole(UserRole.USER)
                .build();

        userRepository.save(user); // DB에 쏙 저장!
        return ResponseEntity.ok("회원가입 성공! (생성된 User ID: " + user.getId() + ")");
    }
}