package com.minierp.backend.domain.approval.entity;

import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "leave_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_type", nullable = false, length = 50)
    private LeaveType appType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "used_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal usedDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_status", nullable = false, length = 50)
    private LeaveStatus appStatus = LeaveStatus.PENDING;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Builder
    public LeaveRequest(User requester, LeaveType appType, LocalDate startDate, LocalDate endDate) {
        this.requester = requester;
        this.appType = appType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.appStatus = LeaveStatus.PENDING;
        calculateUsedDays(Collections.emptyList()); // 기본적으로 공휴일 없이 계산
    }

    /**
     * 연차 승인 처리
     */
    public void approve(User approver) {
        if (this.appStatus != LeaveStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 결재 건입니다. (현재 상태: " + appStatus.getDisplayName() + ")");
        }
        this.approver = approver;
        this.appStatus = LeaveStatus.APPROVED;
        
        // 실제 연차 차감 로직은 Service에서 User.deductAnnualLeave(this.usedDays)로 호출됨
    }

    /**
     * 연차 반려 처리
     */
    public void reject(User approver, String reason) {
        if (this.appStatus != LeaveStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 결재 건입니다. (현재 상태: " + appStatus.getDisplayName() + ")");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("반려 사유를 입력해주세요.");
        }
        this.approver = approver;
        this.rejectReason = reason;
        this.appStatus = LeaveStatus.REJECTED;
    }

    /**
     * 주말을 제외한 실제 연차 소진 일수를 계산 (반차는 0.5 고정)
     */
    public void calculateUsedDays(List<LocalDate> holidayList) {
        if (appType.isHalfDay()) {
            this.usedDays = appType.getUnitDays(); // 0.5
            return;
        }

        long workingDays = startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY)
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SUNDAY)
                .filter(date -> !holidayList.contains(date))
                .count();

        if (workingDays == 0) {
            throw new IllegalArgumentException("신청 기간에 평일이 포함되어 있지 않습니다.");
        }

        this.usedDays = BigDecimal.valueOf(workingDays);
    }

    @PrePersist
    @PreUpdate
    private void validatePeriod() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
    }
}
