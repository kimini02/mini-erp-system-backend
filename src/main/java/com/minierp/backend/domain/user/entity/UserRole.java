package com.minierp.backend.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER("일반 사용자"),
    TEAM_LEADER("팀장"),
    ADMIN("관리 소장");

    private final String displayName;

    public boolean isTopManager() {
        return this == ADMIN;
    }

    public boolean isTeamLeader() {
        return this == TEAM_LEADER;
    }

    public boolean isGeneralUser() {
        return this == USER;
    }

    public static UserRole from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 권한 값입니다.");
        }

        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "USER", "일반사용자", "일반 사용자" -> USER;
            case "TEAM_LEADER", "TEAMLEADER", "팀장" -> TEAM_LEADER;
            case "ADMIN", "MANAGER", "관리소장", "관리 소장", "관리자" -> ADMIN;
            default -> throw new IllegalArgumentException("유효하지 않은 권한 값입니다.");
        };
    }
}
