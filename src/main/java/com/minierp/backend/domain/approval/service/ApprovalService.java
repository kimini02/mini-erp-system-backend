package com.minierp.backend.domain.approval.service;

import com.minierp.backend.domain.approval.dto.LeaveRequestCreateDto;
import com.minierp.backend.domain.approval.dto.LeaveRequestResponseDto;
import com.minierp.backend.domain.approval.dto.RejectRequestDto;
import com.minierp.backend.domain.approval.entity.LeaveRequest;
import com.minierp.backend.domain.approval.repository.LeaveRequestRepository;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .requester(requester)
                .appType(dto.getAppType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        if (requester.getRemainingAnnualLeave().compareTo(leaveRequest.getUsedDays()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_ANNUAL_LEAVE);
        }
        
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return LeaveRequestResponseDto.from(savedRequest);
    }

    /**
     * 연차 단건 조회 (권한 방어)
     */
    public LeaveRequestResponseDto getLeaveRequest(Long requestId, Long accessorId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));
        
        User accessor = userRepository.findById(accessorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // [방어 로직] USER 권한인데 본인 내역이 아니라면 차단
        if (accessor.getUserRole() == UserRole.USER && !leaveRequest.getRequester().getId().equals(accessorId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        return LeaveRequestResponseDto.from(leaveRequest);
    }

    /**
     * 연차 승인
     */
    @Transactional
    public void approveLeaveRequest(Long requestId, Long approverId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (approver.getUserRole() == UserRole.USER) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_APPROVER);
        }

        if (leaveRequest.getRequester().getId().equals(approverId)) {
            throw new BusinessException(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
        }

        leaveRequest.approve(approver);
        User requester = leaveRequest.getRequester();
        requester.deductAnnualLeave(leaveRequest.getUsedDays());
    }

    /**
     * 연차 반려
     */
    @Transactional
    public void rejectLeaveRequest(Long requestId, Long approverId, RejectRequestDto rejectDto) {
        if (rejectDto.getRejectReason() == null || rejectDto.getRejectReason().isBlank()) {
            throw new BusinessException(ErrorCode.REJECT_REASON_REQUIRED);
        }

        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (approver.getUserRole() == UserRole.USER) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_APPROVER);
        }

        if (leaveRequest.getRequester().getId().equals(approverId)) {
            throw new BusinessException(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
        }

        leaveRequest.reject(approver, rejectDto.getRejectReason());
    }

    /**
     * 직급별 연차 기준 반환
     */
    public Map<String, Integer> getLeavePolicy() {
        Map<String, Integer> policy = new LinkedHashMap<>();
        policy.put("사원", 15);
        policy.put("대리", 16);
        policy.put("과장", 17);
        policy.put("차장", 18);
        policy.put("부장", 19);
        return policy;
    }

    /**
     * 연차 내역 조회 (권한별 필터링)
     */
    public List<LeaveRequestResponseDto> getLeaveRequests(Long accessorId) {
        User accessor = userRepository.findById(accessorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // [필터링 로직] USER 권한이면 본인 내역만, 관리자면 전체 내역 조회
        if (accessor.getUserRole() == UserRole.USER) {
            return leaveRequestRepository.findByRequester_Id(accessorId).stream()
                    .map(LeaveRequestResponseDto::from)
                    .collect(Collectors.toList());
        }

        return leaveRequestRepository.findAll().stream()
                .map(LeaveRequestResponseDto::from)
                .collect(Collectors.toList());
    }
}
