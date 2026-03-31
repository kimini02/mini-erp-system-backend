package com.minierp.backend.domain.attendance.entity;

import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendances", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "work_date"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "clock_in_time")
    private LocalTime clockInTime;

    @Column(name = "clock_out_time")
    private LocalTime clockOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "att_status", nullable = false, length = 20)
    private AttendanceStatus attStatus;

    private Attendance(User user, LocalDate workDate, LocalTime clockInTime, AttendanceStatus attStatus) {
        this.user = user;
        this.workDate = workDate;
        this.clockInTime = clockInTime;
        this.attStatus = attStatus;
    }

    public static Attendance checkIn(User user, LocalDate workDate, LocalTime clockInTime) {
        AttendanceStatus status = clockInTime.isAfter(LocalTime.of(9, 0))
                ? AttendanceStatus.LATE
                : AttendanceStatus.NORMAL;
        return new Attendance(user, workDate, clockInTime, status);
    }

    public void checkOut(LocalTime clockOutTime) {
        this.clockOutTime = clockOutTime;
    }

    public void updateAttendance(LocalTime clockInTime, LocalTime clockOutTime) {
        this.clockInTime = clockInTime;
        this.clockOutTime = clockOutTime;
        this.attStatus = clockInTime.isAfter(LocalTime.of(9, 0))
                ? AttendanceStatus.LATE
                : AttendanceStatus.NORMAL;
    }
}
