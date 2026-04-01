package com.minierp.backend.domain.overtime.entity;

import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "overtime_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OvertimeRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Column(name = "overtime_date", nullable = false)
    private LocalDate overtimeDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OvertimeStatus status = OvertimeStatus.PENDING;

    @Builder
    public OvertimeRequest(User requester, LocalDate overtimeDate, LocalTime startTime, LocalTime endTime, String reason) {
        this.requester = requester;
        this.overtimeDate = overtimeDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.status = OvertimeStatus.PENDING;
    }

    public void approve(User approver) {
        if (this.status != OvertimeStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        this.approver = approver;
        this.status = OvertimeStatus.APPROVED;
    }

    public void reject(User approver) {
        if (this.status != OvertimeStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        this.approver = approver;
        this.status = OvertimeStatus.REJECTED;
    }
}
