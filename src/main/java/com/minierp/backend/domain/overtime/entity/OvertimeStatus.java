package com.minierp.backend.domain.overtime.entity;

import lombok.Getter;

@Getter
public enum OvertimeStatus {
    PENDING("결재 대기"),
    APPROVED("승인"),
    REJECTED("반려");

    private final String displayName;

    OvertimeStatus(String displayName) {
        this.displayName = displayName;
    }
}
