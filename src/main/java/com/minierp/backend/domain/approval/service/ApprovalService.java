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
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            "사원", 15,
            "대리", 16,
            "과장", 17,
            "팀장", 18
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

    /**
     * 연차 신청 생성
     */
    @Transactional
    public LeaveRequestResponseDto createLeaveRequest(LeaveRequestCreateDto dto, Long requesterUserId) {
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (containsWeekendOrHoliday(dto.getStartDate(), dto.getEndDate())) {
            throw new BusinessException(ErrorCode.LEAVE_DATE_NOT_WORKING_DAY);
        }

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
     * 특정 사용자의 연차 신청 내역 조회
     */
    public List<LeaveRequestResponseDto> getMyLeaveRequests(Long requesterUserId) {
        User user = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return leaveRequestRepository.findAll().stream()
                .filter(req -> req.getRequester().getId().equals(user.getId()))
                .map(LeaveRequestResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 모든 연차 신청 내역 조회 (관리자용)
     */
    public List<LeaveRequestResponseDto> getAllLeaveRequests(Long requesterUserId) {
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // USER: 본인 내역만 조회
        if (requester.getUserRole().isGeneralUser()) {
            return leaveRequestRepository.findAll().stream()
                    .filter(req -> req.getRequester().getId().equals(requester.getId()))
                    .map(LeaveRequestResponseDto::from)
                    .collect(Collectors.toList());
        }

        // TEAM_LEADER/ADMIN: 전체 내역 조회
        return leaveRequestRepository.findAll().stream()
                .map(LeaveRequestResponseDto::from)
                .collect(Collectors.toList());
    }

    public LeaveBalanceResponseDto getLeaveBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new LeaveBalanceResponseDto(
                user.getTotalAnnualLeave(),
                user.getUsedAnnualLeave(),
                user.getRemainingAnnualLeave()
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

    private void validateApprovalPermission(LeaveRequest leaveRequest, User approver) {
        if (leaveRequest.getAppStatus() != LeaveStatus.PENDING) {
            throw new BusinessException(ErrorCode.LEAVE_ALREADY_PROCESSED,
                    "이미 처리된 결재 건입니다. (현재 상태: " + leaveRequest.getAppStatus().getDisplayName() + ")");
        }

        User requester = leaveRequest.getRequester();

        // 3) 관리소장(ADMIN) 신청 건은 본인만 결재 가능 (셀프 결재 허용)
        if (requester.getUserRole().isTopManager()) {
            if (!requester.getId().equals(approver.getId())) {
                throw new BusinessException(ErrorCode.APPROVAL_ADMIN_ONLY,
                        "관리소장 신청 건은 본인만 결재할 수 있습니다.");
            }
            return;
        }

        // 1) 일반 사용자(USER) 신청 건: 팀장 또는 관리소장이 결재 가능
        if (requester.getUserRole().isGeneralUser()) {
            if (approver.getUserRole().isTeamLeader() || approver.getUserRole().isTopManager()) {
                return;
            }
            throw new BusinessException(ErrorCode.APPROVAL_ADMIN_ONLY,
                    "일반 사용자 신청 건은 팀장 또는 관리소장만 결재할 수 있습니다.");
        }

        // 2) 팀장(TEAM_LEADER) 신청 건: 관리소장만 결재 가능
        if (requester.getUserRole().isTeamLeader()) {
            if (approver.getUserRole().isTopManager()) {
                return;
            }
            throw new BusinessException(ErrorCode.APPROVAL_ADMIN_ONLY,
                    "팀장 신청 건은 관리소장만 결재할 수 있습니다.");
        }

        throw new BusinessException(ErrorCode.APPROVAL_ADMIN_ONLY,
                "권한 정책상 처리할 수 없는 결재 대상입니다.");
    }
}
