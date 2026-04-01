package com.minierp.backend.domain.approval.repository;

import com.minierp.backend.domain.approval.entity.LeaveRequest;
import com.minierp.backend.domain.approval.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByRequester_IdAndAppStatusAndStartDateBetween(
            Long requesterId, LeaveStatus appStatus, LocalDate start, LocalDate end);
}
