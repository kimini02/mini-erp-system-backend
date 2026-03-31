package com.minierp.backend.domain.approval.entity;

import java.math.BigDecimal;

public enum LeaveType {
    ANNUAL("연차", new BigDecimal("1.0")),
    HALF_MORNING("오전반차", new BigDecimal("0.5")),
    HALF_AFTERNOON("오후반차", new BigDecimal("0.5"));

    private final String displayName;
    private final BigDecimal unitDays;

    LeaveType(String displayName, BigDecimal unitDays) {
        this.displayName = displayName;
        this.unitDays = unitDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getUnitDays() {
        return unitDays;
    }

    public boolean isHalfDay() {
        return this == HALF_MORNING || this == HALF_AFTERNOON;
    }
}
