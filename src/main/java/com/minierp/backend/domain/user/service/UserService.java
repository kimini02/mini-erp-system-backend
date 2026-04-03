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
import com.minierp.backend.global.service.AccessPolicy;
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
    private final AccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public UserListResponseDto getUsers(int page, int size, String role, String search, Long requesterUserId) {
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);

        Specification<User> spec;
        if (requester.getUserRole().isTopManager()) {
            spec = isActive();
            spec = spec.and(roleEquals(role));
            spec = spec.and(nameOrEmailContains(search));
        } else {
            spec = isActive();
            spec = spec.and(userIdEquals(requesterUserId));
        }

        Page<UserResponseDto> users = userRepository.findAll(spec, pageable).map(UserResponseDto::detail);
        return UserListResponseDto.of(users);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUser(Long userId, Long requesterUserId) {
        User requester = getUserOrThrow(requesterUserId);
        User target = getUserOrThrow(userId);
        validateSelfOrAdmin(target, requester);
        return UserResponseDto.detail(target);
    }

    @Transactional
    public UserResponseDto updateUser(Long userId, UserUpdateRequestDto requestDto, Long requesterUserId) {
        User requester = getUserOrThrow(requesterUserId);
        User target = getUserOrThrow(userId);
        validateSelfOrAdmin(target, requester);

        target.updateProfile(requestDto.getName(), requestDto.getDepartmentCode(), requestDto.getPosition());
        return UserResponseDto.detail(target);
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

    private void validateSelfOrAdmin(User target, User requester) {
        if (!accessPolicy.canAccessSelfOrAdmin(target.getId(), requester.getId(), requester.getUserRole())) {
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

    private Specification<User> userIdEquals(Long userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("id"), userId);
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
