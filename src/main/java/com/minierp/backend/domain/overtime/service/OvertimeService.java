package com.minierp.backend.domain.overtime.service;

import com.minierp.backend.domain.overtime.dto.OvertimeRequestDto;
import com.minierp.backend.domain.overtime.dto.OvertimeResponseDto;
import com.minierp.backend.domain.overtime.entity.OvertimeRequest;
import com.minierp.backend.domain.overtime.repository.OvertimeRequestRepository;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import com.minierp.backend.global.service.AccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OvertimeService {

    private final OvertimeRequestRepository overtimeRequestRepository;
    private final UserRepository userRepository;
    private final AccessPolicy accessPolicy;

    /**
     * 특근 신청
     */
    @Transactional
    public OvertimeResponseDto requestOvertime(OvertimeRequestDto dto, Long requesterUserId) {
        // 방어 코드: 시작 시간이 종료 시간보다 늦을 경우
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException(ErrorCode.INVALID_OVERTIME_TIME);
        }

        if (!isWeekend(dto.getOvertimeDate())) {
            throw new BusinessException(ErrorCode.INVALID_OVERTIME_DATE);
        }

        User requester = getUserById(requesterUserId);

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
     * - USER: 본인 건만 조회
     * - TEAM_LEADER/ADMIN: 전체 조회 가능
     */
    public OvertimeResponseDto getOvertimeRequest(Long requestId, Long accessorUserId) {
        OvertimeRequest request = getRequestOrThrow(requestId);
        User accessor = getUserById(accessorUserId);

        if (accessor.getUserRole().isGeneralUser() && !request.getRequester().getId().equals(accessor.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인 특근 신청 건만 조회할 수 있습니다.");
        }

        return OvertimeResponseDto.from(request);
    }

    /**
     * 특근 승인
     */
    @Transactional
    public OvertimeResponseDto approveOvertime(Long requestId, Long approverUserId) {
        OvertimeRequest request = getRequestOrThrow(requestId);
        User approver = getUserById(approverUserId);

        validateApprovalHierarchy(request.getRequester(), approver);
        request.approve(approver);
        return OvertimeResponseDto.from(request);
    }

    /**
     * 특근 반려
     */
    @Transactional
    public OvertimeResponseDto rejectOvertime(Long requestId, Long approverUserId) {
        OvertimeRequest request = getRequestOrThrow(requestId);
        User approver = getUserById(approverUserId);

        validateApprovalHierarchy(request.getRequester(), approver);
        request.reject(approver);
        return OvertimeResponseDto.from(request);
    }

    /**
     * 특근 내역 조회 (권한별 필터링)
     * - USER: 본인 내역만
     * - TEAM_LEADER/ADMIN: /all 엔드포인트 사용
     */
    public List<OvertimeResponseDto> getOvertimeRequestList(Long accessorUserId) {
        User accessor = getUserById(accessorUserId);

        // USER가 아니면 권한 거부 (ADMIN/TEAM_LEADER는 /all 사용)
        if (!accessor.getUserRole().isGeneralUser()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "관리자는 /api/v1/overtime/all 엔드포인트를 사용해주세요.");
        }

        // USER는 본인 내역만 반환
        return overtimeRequestRepository.findByRequester_Id(accessor.getId()).stream()
                .map(OvertimeResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특근 전체 내역 조회 (관리자용)
     * - ADMIN/TEAM_LEADER만 접근 가능
     * - 항상 전체 내역 반환
     */
    public List<OvertimeResponseDto> getAllOvertimeRequestList(Long userId) {
        User user = getUserById(userId);

        // ADMIN/TEAM_LEADER만 접근 가능
        if (!accessPolicy.canViewAllRequests(user.getUserRole())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "관리자 권한이 필요합니다.");
        }

        // 항상 전체 내역 반환
        return overtimeRequestRepository.findAll().stream()
                .map(OvertimeResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * [Hierarchy 검증 로직]
     * 1) requester=USER: TEAM_LEADER 또는 ADMIN 결재 가능
     * 2) requester=TEAM_LEADER: ADMIN만 결재 가능
     * 3) requester=ADMIN: ADMIN 본인만 결재 가능(셀프 허용)
     */
    private void validateApprovalHierarchy(User requester, User approver) {
        accessPolicy.validateApprovalHierarchy(
                requester.getUserRole(),
                requester.getId(),
                approver.getUserRole(),
                approver.getId()
        );
    }

    private OvertimeRequest getRequestOrThrow(Long requestId) {
        return overtimeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OVERTIME_NOT_FOUND));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
