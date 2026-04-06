package com.minierp.backend.domain.approval.entity;

import lombok.Getter;

@Getter
public enum LeaveStatus {
    PENDING("결재 대기"),
    APPROVED("승인"),
    REJECTED("반려"),
    CANCELLED("취소");

    private final String displayName;

    LeaveStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean isClosed() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }
}
