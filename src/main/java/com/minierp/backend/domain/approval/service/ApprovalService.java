package com.minierp.backend.domain.approval.service;

import com.minierp.backend.domain.approval.dto.LeaveRequestCreateDto;
import com.minierp.backend.domain.approval.dto.LeaveRequestResponseDto;
import com.minierp.backend.domain.approval.dto.RejectRequestDto;
import com.minierp.backend.domain.approval.entity.LeaveRequest;
import com.minierp.backend.domain.approval.repository.LeaveRequestRepository;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;

    /**
     * 연차 신청 생성
     */
    @Transactional
    public LeaveRequestResponseDto createLeaveRequest(LeaveRequestCreateDto dto, Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .requester(requester)
                .appType(dto.getAppType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        // usedDays 계산 로직은 LeaveRequest 생성자/메서드 내에 포함되어 있음 (주말 제외)
        
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return LeaveRequestResponseDto.from(savedRequest);
    }

    /**
     * 연차 승인 (Absolute Rule: @Transactional 내에서 상태 변경 및 User 연차 차감)
     */
    @Transactional
    public void approveLeaveRequest(Long requestId, Long approverId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("결재 요청을 찾을 수 없습니다."));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("승인자 정보를 찾을 수 없습니다."));

        // 1. LeaveRequest 상태 변경 (PENDING -> APPROVED)
        leaveRequest.approve(approver);

        // 2. User 엔티티의 연차 차감 (Absolute Rule: User에서 관리)
        User requester = leaveRequest.getRequester();
        requester.deductAnnualLeave(leaveRequest.getUsedDays());

        // @Transactional에 의해 변경 사항이 자동 반영됨
    }

    /**
     * 연차 반려
     */
    @Transactional
    public void rejectLeaveRequest(Long requestId, Long approverId, RejectRequestDto rejectDto) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("결재 요청을 찾을 수 없습니다."));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("승인자 정보를 찾을 수 없습니다."));

        leaveRequest.reject(approver, rejectDto.getRejectReason());
    }

    /**
     * 특정 사용자의 연차 신청 내역 조회
     */
    public List<LeaveRequestResponseDto> getMyLeaveRequests(Long userId) {
        return leaveRequestRepository.findAll().stream()
                .filter(req -> req.getRequester().getId().equals(userId))
                .map(LeaveRequestResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 모든 연차 신청 내역 조회 (관리자용)
     */
    public List<LeaveRequestResponseDto> getAllLeaveRequests() {
        return leaveRequestRepository.findAll().stream()
                .map(LeaveRequestResponseDto::from)
                .collect(Collectors.toList());
    }
}
