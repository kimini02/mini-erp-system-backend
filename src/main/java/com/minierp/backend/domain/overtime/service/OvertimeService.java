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

        // [방어 로직] USER 권한인데 본인 내역이 아니라면 차단
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

        if (approver.getUserRole() == UserRole.USER) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_APPROVER);
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

        if (approver.getUserRole() == UserRole.USER) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_APPROVER);
        }

        request.reject(approver);
    }

    /**
     * 특근 내역 조회 (권한별 필터링)
     */
    public List<OvertimeResponseDto> getOvertimeRequests(Long accessorId) {
        User accessor = userRepository.findById(accessorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // [필터링 로직] USER 권한이면 본인 내역만, 관리자면 전체 내역 조회
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
