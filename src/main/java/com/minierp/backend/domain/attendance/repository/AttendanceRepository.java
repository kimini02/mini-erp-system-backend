package com.minierp.backend.domain.attendance.repository;

import com.minierp.backend.domain.attendance.entity.Attendance;
import com.minierp.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserAndWorkDate(User user, LocalDate workDate);

    List<Attendance> findAllByUserAndWorkDateBetweenOrderByWorkDateAsc(User user, LocalDate startDate, LocalDate endDate);
}
