package com.minierp.backend.domain.user.entity;

import lombok.Getter;

@Getter
public enum UserRole {
    USER("일반 사용자"),
    TEAMLEADER("팀장"),
    ADMIN("관리소장");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }
}
