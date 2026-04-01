package com.minierp.backend.domain.overtime.repository;

import com.minierp.backend.domain.overtime.entity.OvertimeRequest;
import com.minierp.backend.domain.overtime.entity.OvertimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OvertimeRequestRepository extends JpaRepository<OvertimeRequest, Long> {
    List<OvertimeRequest> findByRequester_Id(Long requesterId);
    List<OvertimeRequest> findByRequester_IdAndStatusAndOvertimeDateBetween(
            Long requesterId, OvertimeStatus status, LocalDate start, LocalDate end);
}
