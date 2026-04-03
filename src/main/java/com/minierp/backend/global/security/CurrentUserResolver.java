package com.minierp.backend.global.security;

import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    public Long resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증 사용자 정보가 올바르지 않습니다.");
        }
    }

    public UserRole resolveUserRole(Authentication authentication) {
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

    public boolean isTopManager(Authentication authentication) {
        return resolveUserRole(authentication).isTopManager();
    }
}

