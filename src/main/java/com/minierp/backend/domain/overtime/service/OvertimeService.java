package com.minierp.backend.domain.overtime.service;

import com.minierp.backend.domain.overtime.dto.OvertimeRequestDto;
import com.minierp.backend.domain.overtime.dto.OvertimeResponseDto;
import com.minierp.backend.domain.overtime.entity.OvertimeRequest;
import com.minierp.backend.domain.overtime.entity.OvertimeStatus;
import com.minierp.backend.domain.overtime.repository.OvertimeRequestRepository;
import com.minierp.backend.domain.user.entity.User;
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
        // 방어 코드: 시작 시간이 종료 시간보다 늦을 경우
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
     * 특근 승인
     */
    @Transactional
    public void approveOvertime(Long requestId, Long approverId) {
        OvertimeRequest request = overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OVERTIME_NOT_FOUND));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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

        request.reject(approver);
    }

    /**
     * 내 특근 내역 조회
     */
    public List<OvertimeResponseDto> getMyOvertimeRequests(Long userId) {
        return overtimeRequestRepository.findByRequester_Id(userId).stream()
                .map(OvertimeResponseDto::from)
                .collect(Collectors.toList());
    }
}
