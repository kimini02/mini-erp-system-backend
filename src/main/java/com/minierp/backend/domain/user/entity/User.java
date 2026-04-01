package com.minierp.backend.domain.user.entity;

import com.minierp.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(name = "user_email", nullable = false, unique = true, length = 100)
    private String userEmail;

    @Column(name = "user_pw", nullable = false, length = 255)
    private String userPw;

    @Column(name = "position_name", nullable = false, length = 30)
    private String positionName;

    @Column(name = "assign_role", length = 30)
    private String assignRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    private UserRole userRole;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "total_annual_leave", nullable = false, precision = 5, scale = 1)
    private BigDecimal totalAnnualLeave;

    @Column(name = "used_annual_leave", nullable = false, precision = 5, scale = 1)
    private BigDecimal usedAnnualLeave;

    @Column(name = "remaining_annual_leave", nullable = false, precision = 5, scale = 1)
    private BigDecimal remainingAnnualLeave;

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    private User(String userName, String loginId, String userEmail, String userPw, String positionName, UserRole userRole) {
        this.userName = userName;
        this.loginId = loginId;
        this.userEmail = userEmail;
        this.userPw = userPw;
        this.positionName = positionName;
        this.assignRole = null;
        this.userRole = userRole;
        this.isActive = true;
        this.totalAnnualLeave = BigDecimal.valueOf(15.0);
        this.usedAnnualLeave = BigDecimal.ZERO;
        this.remainingAnnualLeave = BigDecimal.valueOf(15.0);
    }

    public static User create(String userName, String loginId, String userEmail, String encodedPassword, String positionName) {
        return new User(userName, loginId, userEmail, encodedPassword, positionName, UserRole.USER);
    }

    public void updateProfile(String userName, String positionName) {
        this.userName = userName;
        this.positionName = positionName;
    }

    public void changeRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public boolean passwordMatches(String encodedPassword) {
        return userPw.equals(encodedPassword);
    }

    public void updatePassword(String newEncodedPassword) {
        this.userPw = newEncodedPassword;
    }

    public void deductAnnualLeave(BigDecimal usedDays) {
        if (usedDays == null || usedDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("차감할 연차 일수가 유효하지 않습니다.");
        }
        if (remainingAnnualLeave.compareTo(usedDays) < 0) {
            throw new IllegalArgumentException("잔여 연차가 부족합니다.");
        }

        this.usedAnnualLeave = this.usedAnnualLeave.add(usedDays);
        this.remainingAnnualLeave = this.remainingAnnualLeave.subtract(usedDays);
    }
}
