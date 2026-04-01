package com.minierp.backend.domain.approval.repository;

import com.minierp.backend.domain.approval.entity.LeaveRequest;
import com.minierp.backend.domain.approval.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByRequester_IdAndAppStatusAndStartDateBetween(
            Long requesterId, LeaveStatus appStatus, LocalDate start, LocalDate end);
    // [버그 수정] 특정 기간과 겹치는 모든 승인된 연차 조회 (월을 걸치는 연차 포함)
    @Query("SELECT lr FROM LeaveRequest lr " +
            "WHERE lr.appStatus = :appStatus " +
            "AND lr.startDate <= :endDate " +
            "AND lr.endDate >= :startDate")
    List<LeaveRequest> findOverlappingLeaves(
            @Param("appStatus") LeaveStatus appStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT lr FROM LeaveRequest lr " +
            "WHERE lr.requester.id = :requesterId " +
            "AND lr.appStatus = :appStatus " +
            "AND lr.startDate <= :endDate " +
            "AND lr.endDate >= :startDate")
    List<LeaveRequest> findOverlappingLeavesByRequester(
            @Param("requesterId") Long requesterId,
            @Param("appStatus") LeaveStatus appStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<LeaveRequest> findByRequester_Id(Long requesterId);
}
