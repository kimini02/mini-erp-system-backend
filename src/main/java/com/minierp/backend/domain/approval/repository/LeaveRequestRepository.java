package com.minierp.backend.domain.approval.repository;

import com.minierp.backend.domain.approval.entity.LeaveRequest;
import com.minierp.backend.domain.approval.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByRequester_IdAndAppStatusAndStartDateBetween(
            Long requesterId, LeaveStatus appStatus, LocalDate start, LocalDate end);
            
    // 전사 캘린더용: 특정 기간의 모든 승인된 연차 조회
    List<LeaveRequest> findByAppStatusAndStartDateBetween(LeaveStatus appStatus, LocalDate start, LocalDate end);
    
    // 본인 내역 필터링용
    List<LeaveRequest> findByRequester_Id(Long requesterId);
}
