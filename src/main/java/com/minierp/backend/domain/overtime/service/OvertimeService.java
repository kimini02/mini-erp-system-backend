package com.minierp.backend.domain.overtime.service;

import com.minierp.backend.domain.overtime.dto.OvertimeRequestDto;
import com.minierp.backend.domain.overtime.dto.OvertimeResponseDto;
import com.minierp.backend.domain.overtime.entity.OvertimeRequest;
import com.minierp.backend.domain.overtime.entity.OvertimeStatus;
import com.minierp.backend.domain.overtime.repository.OvertimeRequestRepository;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OvertimeService {

    private final OvertimeRequestRepository overtimeRequestRepository;
    private final UserRepository userRepository;

    /**
     * 특근 신청
     */
    @Transactional
    public OvertimeResponseDto requestOvertime(OvertimeRequestDto dto, Long requesterId) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException(ErrorCode.INVALID_OVERTIME_TIME);
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        OvertimeRequest request = OvertimeRequest.builder()
                .requester(requester)
                .overtimeDate(dto.getOvertimeDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .reason(dto.getReason())
                .build();

        return OvertimeResponseDto.from(overtimeRequestRepository.save(request));
    }

    /**
     * 특근 단건 조회 (권한 방어)
     */
    public OvertimeResponseDto getOvertimeRequest(Long requestId, Long accessorId) {
        OvertimeRequest request = overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OVERTIME_NOT_FOUND));
        
        User accessor = userRepository.findById(accessorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // [방어 로직] 일반 사원(USER)은 본인 내역만 조회 가능. 팀장(TEAMLEADER)과 관리소장(ADMIN)은 타인 내역 조회 가능.
        if (accessor.getUserRole() == UserRole.USER && !request.getRequester().getId().equals(accessorId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        return OvertimeResponseDto.from(request);
    }

    /**
     * 특근 승인
     */
    @Transactional
    public void approveOvertime(Long requestId, Long approverId) {
        OvertimeRequest request = overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OVERTIME_NOT_FOUND));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // [Hierarchy 검증]
        validateApprovalHierarchy(request.getRequester(), approver);

        // [비즈니스 규칙 1] 본인 결재 차단 (단, 관리소장 ADMIN은 예외적으로 셀프 승인 허용)
        if (request.getRequester().getId().equals(approverId) && approver.getUserRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
        }

        request.approve(approver);
    }

    /**
     * 특근 반려
     */
    @Transactional
    public void rejectOvertime(Long requestId, Long approverId) {
        OvertimeRequest request = overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OVERTIME_NOT_FOUND));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // [Hierarchy 검증]
        validateApprovalHierarchy(request.getRequester(), approver);

        // [비즈니스 규칙 1] 본인 결재 차단 (단, 관리소장 ADMIN은 예외적으로 셀프 반려 허용)
        if (request.getRequester().getId().equals(approverId) && approver.getUserRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
        }

        request.reject(approver);
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
     * 특근 내역 조회 (권한별 필터링)
     */
    public List<OvertimeResponseDto> getOvertimeRequests(Long accessorId) {
        User accessor = userRepository.findById(accessorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // [필터링 로직] USER 권한이면 본인 내역만, TEAMLEADER/ADMIN 권한이면 전체 내역 조회 가능
        if (accessor.getUserRole() == UserRole.USER) {
            return overtimeRequestRepository.findByRequester_Id(accessorId).stream()
                    .map(OvertimeResponseDto::from)
                    .collect(Collectors.toList());
        }

        return overtimeRequestRepository.findAll().stream()
                .map(OvertimeResponseDto::from)
                .collect(Collectors.toList());
    }
}
