package com.minierp.backend.domain.user.service;

import com.minierp.backend.domain.user.dto.UserListResponseDto;
import com.minierp.backend.domain.user.dto.UserResponseDto;
import com.minierp.backend.domain.user.dto.UserRoleUpdateResponseDto;
import com.minierp.backend.domain.user.dto.UserUpdateRequestDto;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserListResponseDto getUsers(int page, int size, String role, String search, String requesterLoginId) {
        User requester = userRepository.findByLoginId(requesterLoginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);

        Specification<User> spec;
        if (requester.getUserRole().isTopManager()) {
            spec = Specification.where(isActive()).and(roleEquals(role)).and(nameOrEmailContains(search));
        } else {
            spec = Specification.where(isActive()).and(loginIdEquals(requesterLoginId));
        }

        Page<UserResponseDto> users = userRepository.findAll(spec, pageable).map(UserResponseDto::detail);
        return UserListResponseDto.of(users);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUser(Long userId, String requesterLoginId, boolean isAdmin) {
        User user = getUserOrThrow(userId);
        validateSelfOrAdmin(user, requesterLoginId, isAdmin);
        return UserResponseDto.detail(user);
    }

    @Transactional
    public UserResponseDto updateUser(Long userId, UserUpdateRequestDto requestDto, String requesterLoginId, boolean isAdmin) {
        User user = getUserOrThrow(userId);
        validateSelfOrAdmin(user, requesterLoginId, isAdmin);

        user.updateProfile(requestDto.getName(), requestDto.getPosition());
        return UserResponseDto.detail(user);
    }

    @Transactional
    public UserRoleUpdateResponseDto updateUserRole(Long userId, String role) {
        User user = getUserOrThrow(userId);
        user.changeRole(parseUserRole(role));
        return UserRoleUpdateResponseDto.from(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateSelfOrAdmin(User user, String requesterLoginId, boolean isAdmin) {
        if (!isAdmin && !user.getLoginId().equals(requesterLoginId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private Specification<User> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    private Specification<User> roleEquals(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        UserRole userRole = parseUserRole(role);
        return (root, query, cb) -> cb.equal(root.get("userRole"), userRole);
    }

    private Specification<User> loginIdEquals(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("loginId"), loginId);
    }

    private Specification<User> nameOrEmailContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("userName")), pattern),
                cb.like(cb.lower(root.get("userEmail")), pattern)
        );
    }

    private UserRole parseUserRole(String role) {
        try {
            return UserRole.from(role);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 권한 값입니다.");
        }
    }
}
