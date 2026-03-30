# 이세연 - 연차/결재 파트

## 담당 도메인
- `domain/approval/` (연차 신청 + 전자결재)

---

## 해야 할 일 요약

### 1순위: 연차 신청
| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/v1/leave/requests` | 연차 신청 | USER, usedDays는 서버 계산 |
| `GET /api/v1/leave/balance` | 잔여 연차 조회 | 본인의 총/사용/잔여 연차 |
| `GET /api/v1/leave/policy` | 직급별 연차 기준 조회 | 사원 15일, 대리 16일 등 |
| `GET /api/v1/leave/requests/me` | 내 연차 신청 내역 | 본인 신청 목록 (상태/반려사유 포함) |

### 2순위: 연차 승인/반려 (ADMIN)
| API | 메서드 | 설명 |
|---|---|---|
| `GET /api/v1/leave/requests` | 전체 연차 신청 조회 | ADMIN만, status 필터 가능 |
| `PATCH /api/v1/leave/requests/{id}/approve` | 연차 승인 | ADMIN만, 승인 즉시 연차 차감 |
| `PATCH /api/v1/leave/requests/{id}/reject` | 연차 반려 | ADMIN만, 반려 사유 필수 |

---

## 핵심 비즈니스 규칙 (반드시 지킬 것)

### usedDays 서버 계산
- 클라이언트가 보낸 값을 신뢰하지 않음
- 서버에서 startDate ~ endDate 사이 **주말/공휴일 제외한 평일만** 계산
- 반차(HALF)는 무조건 **0.5일** 고정

### 주말/공휴일 검증
- 신청 기간에 주말/공휴일이 포함되면 **신청 자체를 거부** (422 에러)
- UI 차단 + 서버 재검증 이중 방어

### 승인-차감 트랜잭션 (가장 중요)
```
1) 신청 상태 검증 (PENDING인지 확인)
2) appStatus = APPROVED로 변경
3) User.remainingAnnualLeave -= usedDays (차감)
4) 위 2~3을 하나의 @Transactional로 묶기
5) 하나라도 실패하면 전체 롤백
```

### 잔여 연차 부족 체크
- remainingAnnualLeave < 신청 usedDays이면 **신청 자체 불가** (422 에러)

### 반려 시
- rejectReason(반려 사유) **필수 입력** (빈 값이면 예외)
- 본인이 본인 결재를 승인할 수 없음 (requester_id != approver_id)

---

## 구현 시 주의사항
- 연차 데이터(총/사용/잔여)는 **User 테이블**에서 관리 -> 김보민 파트 User Entity와 연동
- LeaveRequest Entity의 `approve()`, `reject()`, `calculateUsedDays()` 메서드 참고 (Entity.md)
- 연차 잔여 조회(`/leave/balance`)는 User의 연차 필드를 읽어오는 것
- LeaveType: ANNUAL(연차), HALF(반차)
- LeaveStatus: PENDING(대기), APPROVED(승인), REJECTED(반려)

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
```
