# 이세연 - 연차/근태/캘린더 파트

## 담당 도메인
- `domain/approval/` (연차 신청/조회, 승인/반려, 승인 시 차감 트랜잭션)
- `domain/attendance/` (근태 조회, 출근 체크인)
- 캘린더 조회 (attendance 또는 별도 API)

---

## 해야 할 일 요약

### 1순위: 연차 신청
| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/v1/leave/requests` | 연차 신청 | USER, usedDays는 서버 계산 |
| `GET /api/v1/leave/balance` | 잔여 연차 조회 | 본인의 총/사용/잔여 연차 |
| `GET /api/v1/leave/policy` | 직급별 연차 기준 | 사원 15일, 대리 16일 등 |
| `GET /api/v1/leave/requests/me` | 내 신청 내역 | 본인 목록 (상태/반려사유 포함) |

### 2순위: 연차 승인/반려 (ADMIN)
| API | 메서드 | 설명 |
|---|---|---|
| `GET /api/v1/leave/requests` | 전체 신청 조회 | ADMIN만, status 필터 |
| `PATCH /api/v1/leave/requests/{id}/approve` | 승인 | 승인 즉시 연차 차감 |
| `PATCH /api/v1/leave/requests/{id}/reject` | 반려 | 반려 사유 필수 |

### 3순위: 근태
| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/v1/attendance/check-in` | 출근 체크인 | 주말/공휴일 특근 승인 검증 |
| `GET /api/v1/attendance/summary` | 근태 요약 | 월별 출근일수, 지각 등 |

### 4순위: 캘린더
| API | 메서드 | 설명 |
|---|---|---|
| `GET /api/v1/calendar/events` | 캘린더 이벤트 조회 | 월별 Task 일정 + 휴가 일정 통합 |

---

## 핵심 비즈니스 규칙 (반드시 지킬 것)

### usedDays 서버 계산
- 클라이언트 값 신뢰하지 않음
- startDate ~ endDate 사이 **주말/공휴일 제외 평일만** 계산
- 반차(HALF)는 무조건 **0.5일** 고정

### 주말/공휴일 검증
- 신청 기간에 주말/공휴일 포함 시 **신청 거부** (422 에러)

### 승인-차감 트랜잭션 (가장 중요)
```
1) 신청 상태 검증 (PENDING인지 확인)
2) appStatus = APPROVED로 변경
3) User.remainingAnnualLeave -= usedDays (차감)
4) 위 2~3을 하나의 @Transactional로 묶기
5) 하나라도 실패하면 전체 롤백
```

### 기타 규칙
- 잔여 연차 < 신청일수 -> 신청 불가 (422)
- 반려 시 rejectReason 필수 (빈 값 예외)
- 본인이 본인 결재 승인 불가 (requester_id != approver_id)
- 주말/공휴일 출근은 특근 승인 사용자만 허용

---

## 구현 시 주의사항
- 연차 데이터(총/사용/잔여)는 **User 테이블**에서 관리 -> 김보민 파트 User Entity 연동
- 캘린더 이벤트는 Task 일정 + 승인된 휴가 일정을 합쳐서 반환
- LeaveType: ANNUAL(연차), HALF(반차)
- LeaveStatus: PENDING(대기), APPROVED(승인), REJECTED(반려)
- AttendanceStatus: NORMAL(정상), LATE(지각), ABSENT(결근), LEAVE(휴가)

---

## 작업 파일 목록
```
domain/approval/entity/LeaveRequest.java
domain/approval/entity/LeaveStatus.java
domain/approval/entity/LeaveType.java
domain/approval/controller/ApprovalController.java
domain/approval/service/ApprovalService.java
domain/approval/repository/LeaveRequestRepository.java
domain/approval/dto/LeaveRequestCreateDto.java
domain/approval/dto/LeaveRequestResponseDto.java
domain/approval/dto/RejectRequestDto.java
domain/attendance/entity/Attendance.java
domain/attendance/entity/AttendanceStatus.java
domain/attendance/controller/AttendanceController.java
domain/attendance/service/AttendanceService.java
domain/attendance/repository/AttendanceRepository.java
domain/attendance/dto/CheckInRequestDto.java
domain/attendance/dto/AttendanceSummaryDto.java
domain/calendar/controller/CalendarController.java
domain/calendar/service/CalendarService.java
domain/calendar/dto/CalendarEventResponseDto.java
```
