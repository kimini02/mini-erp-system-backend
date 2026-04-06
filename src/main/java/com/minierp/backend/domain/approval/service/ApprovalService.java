package com.minierp.backend.domain.approval.service;

import com.minierp.backend.domain.approval.dto.LeaveBalanceResponseDto;
import com.minierp.backend.domain.approval.dto.LeavePolicyResponseDto;
import com.minierp.backend.domain.approval.dto.LeaveRequestCreateDto;
import com.minierp.backend.domain.approval.dto.LeaveRequestResponseDto;
import com.minierp.backend.domain.approval.dto.RejectRequestDto;
import com.minierp.backend.domain.approval.entity.LeaveRequest;
import com.minierp.backend.domain.approval.entity.LeaveStatus;
import com.minierp.backend.domain.approval.repository.LeaveRequestRepository;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.repository.UserRepository;
import com.minierp.backend.global.exception.BusinessException;
import com.minierp.backend.global.exception.ErrorCode;
import com.minierp.backend.global.service.AccessPolicy;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private static final Map<String, Integer> POSITION_POLICY = Map.of(
            "사원", 14,
            "대리", 16,
            "과장", 17,
            "팀장", 18,
            "관리소장", 19
    );

    // 공휴일 연동 전까지는 고정 공휴일 + 설정 공휴일(app.holidays)을 함께 사용
    private static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(3, 1),
            MonthDay.of(5, 5),
            MonthDay.of(6, 6),
            MonthDay.of(8, 15),
            MonthDay.of(10, 3),
            MonthDay.of(10, 9),
            MonthDay.of(12, 25)
    );

    @Value("${app.holidays:}")
    private String holidaysProperty;

    private Set<LocalDate> configuredHolidays = Set.of();

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final AccessPolicy accessPolicy;

    /**
     * 연차 신청 생성
     */
    @Transactional
    public LeaveRequestResponseDto createLeaveRequest(LeaveRequestCreateDto dto, Long requesterUserId) {
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate startDate = dto.getStartDate();
        LocalDate endDate = dto.getEndDate();
        if (dto.getAppType() != null && dto.getAppType().isHalfDay()) {
            endDate = startDate;
        }

        // 과거 날짜 신청 방지: 시작일이 오늘 이전이면 신청 불가
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "오늘 이전 날짜로는 연차를 신청할 수 없습니다.");
        }

        if (containsWeekendOrHoliday(startDate, endDate)) {
            throw new BusinessException(ErrorCode.LEAVE_DATE_NOT_WORKING_DAY);
        }

        if (hasOverlappingActiveLeaveRequest(requesterUserId, startDate, endDate)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 신청한 연차가 존재합니다.");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .requester(requester)
                .appType(dto.getAppType())
                .startDate(startDate)
                .endDate(endDate)
                .requestReason(dto.getRequestReason())
                .build();

        leaveRequest.calculateUsedDays(getObservedHolidays(startDate.getYear()).stream().toList());

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return LeaveRequestResponseDto.from(savedRequest);
    }


    /**
     * 연차 승인 (Absolute Rule: @Transactional 내에서 상태 변경 및 User 연차 차감)
     */
    @Transactional
    public LeaveRequestResponseDto approveLeaveRequest(Long requestId, Long approverUserId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "결재 요청을 찾을 수 없습니다."));

        User approver = userRepository.findById(approverUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "승인자 정보를 찾을 수 없습니다."));

        validateApprovalPermission(leaveRequest, approver);

        // 1. LeaveRequest 상태 변경 (PENDING -> APPROVED)
        leaveRequest.approve(approver);

        // 2. User 엔티티의 연차 차감 (Absolute Rule: User에서 관리)
        User requester = leaveRequest.getRequester();
        requester.deductAnnualLeave(leaveRequest.getUsedDays());

        return LeaveRequestResponseDto.from(leaveRequest);
    }

    /**
     * 연차 반려
     */
    @Transactional
    public LeaveRequestResponseDto rejectLeaveRequest(Long requestId, Long approverUserId, RejectRequestDto rejectDto) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "결재 요청을 찾을 수 없습니다."));

        User approver = userRepository.findById(approverUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "승인자 정보를 찾을 수 없습니다."));

        validateApprovalPermission(leaveRequest, approver);

        if (rejectDto == null || rejectDto.getRejectReason() == null || rejectDto.getRejectReason().isBlank()) {
            throw new BusinessException(ErrorCode.REJECT_REASON_REQUIRED);
        }

        leaveRequest.reject(approver, rejectDto.getRejectReason());
        return LeaveRequestResponseDto.from(leaveRequest);
    }

    /**
     * 연차 취소
     */
    @Transactional
    public LeaveRequestResponseDto cancelLeaveRequest(Long requestId, Long requesterUserId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "결재 요청을 찾을 수 없습니다."));

        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        if (!leaveRequest.getRequester().getId().equals(requester.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인 신청 건만 취소할 수 있습니다.");
        }

        leaveRequest.cancel(requester);
        return LeaveRequestResponseDto.from(leaveRequest);
    }

    /**
     * 특정 사용자의 연차 신청 내역 조회
     */
    public List<LeaveRequestResponseDto> getMyLeaveRequestList(Long requesterUserId, boolean includeCancelled) {
        User user = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return leaveRequestRepository.findByRequester_Id(user.getId()).stream()
                .filter(req -> includeCancelled || req.getAppStatus() != LeaveStatus.CANCELLED)
                .map(LeaveRequestResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 모든 연차 신청 내역 조회 (관리자용)
     */
    public List<LeaveRequestResponseDto> getAllLeaveRequestList(Long requesterUserId, boolean includeCancelled) {
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!accessPolicy.canViewAllRequests(requester.getUserRole())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "전체 연차 내역은 팀장/관리자만 조회할 수 있습니다.");
        }

        return leaveRequestRepository.findAll().stream()
                .filter(req -> includeCancelled || req.getAppStatus() != LeaveStatus.CANCELLED)
                .map(LeaveRequestResponseDto::from)
                .collect(Collectors.toList());
    }

    public LeaveBalanceResponseDto getLeaveBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // [버그 수정] PENDING 상태인 연차 일수를 합산하여 잔여 연차에서 차감 (실질 잔여 연차 동기화)
        BigDecimal pendingDays = leaveRequestRepository.findByRequester_Id(userId).stream()
                .filter(req -> req.getAppStatus() == LeaveStatus.PENDING)
                .map(LeaveRequest::getUsedDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LeaveBalanceResponseDto(
                user.getTotalAnnualLeave(),
                user.getUsedAnnualLeave(),
                user.getRemainingAnnualLeave().subtract(pendingDays)
        );
    }

    public List<LeavePolicyResponseDto> getLeavePolicy() {
        return POSITION_POLICY.entrySet().stream()
                .map(entry -> new LeavePolicyResponseDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @PostConstruct
    void initConfiguredHolidays() {
        if (holidaysProperty == null || holidaysProperty.isBlank()) {
            configuredHolidays = Set.of();
            return;
        }

        Set<LocalDate> parsed = new HashSet<>();
        Arrays.stream(holidaysProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(LocalDate::parse)
                .forEach(parsed::add);

        configuredHolidays = Set.copyOf(parsed);
    }

    private boolean containsWeekendOrHoliday(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .anyMatch(date -> isWeekend(date) || isHoliday(date));
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private boolean isHoliday(LocalDate date) {
        return getObservedHolidays(date.getYear()).contains(date);
    }

    // 고정 공휴일 + 설정 공휴일 + 주말 겹침 대체공휴일을 하나의 집합으로 구성
    private Set<LocalDate> getObservedHolidays(int year) {
        Set<LocalDate> holidays = new HashSet<>(configuredHolidays);

        for (MonthDay fixedHoliday : FIXED_HOLIDAYS) {
            LocalDate holidayDate = fixedHoliday.atYear(year);
            holidays.add(holidayDate);
        }

        // 고정 공휴일이 주말과 겹치면 다음 평일을 대체공휴일로 추가
        for (MonthDay fixedHoliday : FIXED_HOLIDAYS) {
            LocalDate holidayDate = fixedHoliday.atYear(year);
            if (!isWeekend(holidayDate)) {
                continue;
            }

            LocalDate substituteDate = holidayDate.plusDays(1);
            while (isWeekend(substituteDate) || holidays.contains(substituteDate)) {
                substituteDate = substituteDate.plusDays(1);
            }
            holidays.add(substituteDate);
        }

        return holidays;
    }

    // PENDING/APPROVED 상태의 겹치는 신청이 있으면 중복 신청으로 간주
    private boolean hasOverlappingActiveLeaveRequest(Long requesterUserId, LocalDate startDate, LocalDate endDate) {
        boolean hasPending = !leaveRequestRepository
                .findOverlappingLeavesByRequester(requesterUserId, LeaveStatus.PENDING, startDate, endDate)
                .isEmpty();

        if (hasPending) {
            return true;
        }

        return !leaveRequestRepository
                .findOverlappingLeavesByRequester(requesterUserId, LeaveStatus.APPROVED, startDate, endDate)
                .isEmpty();
    }

    private void validateApprovalPermission(LeaveRequest leaveRequest, User approver) {
        if (leaveRequest.getAppStatus() != LeaveStatus.PENDING) {
            throw new BusinessException(ErrorCode.LEAVE_ALREADY_PROCESSED,
                    "이미 처리된 결재 건입니다. (현재 상태: " + leaveRequest.getAppStatus().getDisplayName() + ")");
        }

        User requester = leaveRequest.getRequester();
        accessPolicy.validateApprovalHierarchy(
                requester.getUserRole(),
                requester.getId(),
                approver.getUserRole(),
                approver.getId()
        );
    }
}
