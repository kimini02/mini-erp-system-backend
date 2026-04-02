package com.minierp.backend.domain.attendance.repository;

import com.minierp.backend.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
}
