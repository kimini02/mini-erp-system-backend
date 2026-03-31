package com.minierp.backend.domain.user.entity;

import com.minierp.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(name = "user_email", unique = true, nullable = false, length = 100)
    private String userEmail;

    @Column(name = "user_pw", nullable = false, length = 255)
    private String userPw;

    @Column(name = "position_name", nullable = false, length = 30)
    private String positionName;

    @Column(name = "assign_role", length = 30)
    private String assignRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // 연차 관련 필드 (Absolute Rule: User 엔티티에서 관리)
    @Column(name = "total_annual_leave", nullable = false, precision = 4, scale = 1)
    private BigDecimal totalAnnualLeave = new BigDecimal("15.0");

    @Column(name = "used_annual_leave", nullable = false, precision = 4, scale = 1)
    private BigDecimal usedAnnualLeave = BigDecimal.ZERO;

    @Column(name = "remaining_annual_leave", nullable = false, precision = 4, scale = 1)
    private BigDecimal remainingAnnualLeave = new BigDecimal("15.0");

    @Builder
    public User(String userName, String userEmail, String userPw, String positionName, String assignRole, UserRole userRole) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPw = userPw;
        this.positionName = positionName;
        this.assignRole = assignRole;
        this.userRole = userRole;
        this.isActive = true;
    }

    /**
     * 연차 차감 로직
     */
    public void deductAnnualLeave(BigDecimal days) {
        if (remainingAnnualLeave.compareTo(days) < 0) {
            throw new IllegalArgumentException("잔여 연차가 부족합니다. (현재 잔여: " + remainingAnnualLeave + "일)");
        }
        this.usedAnnualLeave = this.usedAnnualLeave.add(days);
        this.remainingAnnualLeave = this.remainingAnnualLeave.subtract(days);
    }

    /**
     * 연차 복구 로직 (승인 취소나 반려 시 필요할 수 있음)
     */
    public void restoreAnnualLeave(BigDecimal days) {
        this.usedAnnualLeave = this.usedAnnualLeave.subtract(days);
        this.remainingAnnualLeave = this.remainingAnnualLeave.add(days);
    }
}
