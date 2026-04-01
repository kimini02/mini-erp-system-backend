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

        // [방어 로직] 일반 사원(USER)은 본인 내역만 조회 가능. 팀장(TEAMLEADER)과 관리소장(ADMIN)은 타인 내역 조회 가능.
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

        // [Hierarchy 검증]
        validateApprovalHierarchy(leaveRequest.getRequester(), approver);

        // [비즈니스 규칙 1] 본인 결재 차단 (단, 관리소장 ADMIN은 예외적으로 셀프 승인 허용)
        if (leaveRequest.getRequester().getId().equals(approverId) && approver.getUserRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
        }

        // 1. LeaveRequest 상태 변경
        leaveRequest.approve(approver);

        // 2. User 엔티티의 연차 차감
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

        // [Hierarchy 검증]
        validateApprovalHierarchy(leaveRequest.getRequester(), approver);

        // [비즈니스 규칙 1] 본인 결재 차단 (단, 관리소장 ADMIN은 예외적으로 셀프 반려 허용)
        if (leaveRequest.getRequester().getId().equals(approverId) && approver.getUserRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
        }

        leaveRequest.reject(approver, rejectDto.getRejectReason());
    }

    /**
     * [Hierarchy 검증 로직]
     * 1. 일반 사원(USER)일 때: 팀장(TEAMLEADER) 또는 관리소장(ADMIN)이 결재 가능
     * 2. 팀장(TEAMLEADER)일 때: 관리소장(ADMIN)만 결재 가능
     * 3. 관리소장(ADMIN)일 때: 본인이 직접 결재 가능 (상위 권한 없음)
     */
    private void validateApprovalHierarchy(User requester, User approver) {
        // 결재자가 일반 사원이면 무조건 차단
        if (approver.getUserRole() == UserRole.USER) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_APPROVER);
        }

        // 팀장 이상의 신청 건에 대해 팀장이 결재를 시도할 경우 차단
        if (requester.getUserRole() == UserRole.TEAMLEADER && approver.getUserRole() == UserRole.TEAMLEADER) {
            // 본인 결재 차단 로직에서 관리소장이 아닌 경우 이미 걸러지지만, 하계 계층 구조 명시를 위해 추가
            if (!requester.getId().equals(approver.getId())) {
                 throw new BusinessException(ErrorCode.UNAUTHORIZED_APPROVER);
            }
        }
        
        // 팀장의 상위 결재자는 관리소장(ADMIN)만 가능
        if (requester.getUserRole() == UserRole.TEAMLEADER && approver.getUserRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_APPROVER);
        }
        
        // 관리소장의 신청 건은 관리소장 본인만 처리 가능
        if (requester.getUserRole() == UserRole.ADMIN && approver.getUserRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_APPROVER);
        }
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

        // [필터링 로직] USER 권한이면 본인 내역만, TEAMLEADER/ADMIN 권한이면 전체 내역 조회 가능
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
