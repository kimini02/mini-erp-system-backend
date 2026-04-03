package com.minierp.backend.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DepartmentCode {
    DEVELOPMENT("01", "개발팀"),
    MAINTENANCE("02", "유지보수팀"),
    MOBILE("03", "모바일개발팀");

    private final String code;
    private final String displayName;

    public static DepartmentCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 부서 코드입니다.");
        }

        String normalized = code.trim();
        for (DepartmentCode value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 부서 코드입니다.");
    }
}

