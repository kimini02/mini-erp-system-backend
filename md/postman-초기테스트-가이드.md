# Postman 초기 테스트 가이드 (DB 초기화 후)

DB를 비운 상태에서 회원가입부터 인증/권한/연차/특근/캘린더까지 빠르게 점검하는 순서입니다.

## 0) 서버 실행

```powershell
cd C:\mini-erp-system-backend
.\mvnw.cmd spring-boot:run
```

기본 URL: `http://localhost:8080`

---

## 1) Postman 환경 변수 만들기

Environment 변수:
- `baseUrl` = `http://localhost:8080`
- `managerToken` = (초기 빈값)
- `leaderToken` = (초기 빈값)
- `userToken` = (초기 빈값)
- `leaveId` = (초기 빈값)
- `overtimeId` = (초기 빈값)
- `verificationCode` = (초기 빈값)
- `resetProof` = (초기 빈값)

---

## 2) 회원가입 (3명)

### 2-1) 관리소장 계정 생성
`POST {{baseUrl}}/api/v1/auth/signup`

```json
{
  "id": "manager01",
  "name": "관리소장",
  "email": "manager01@test.com",
  "password": "Password123!",
  "position": "관리소장"
}
```

### 2-2) 팀장 계정 생성
`POST {{baseUrl}}/api/v1/auth/signup`

```json
{
  "id": "leader01",
  "name": "팀장",
  "email": "leader01@test.com",
  "password": "Password123!",
  "position": "팀장"
}
```

### 2-3) 일반 사용자 계정 생성
`POST {{baseUrl}}/api/v1/auth/signup`

```json
{
  "id": "user01",
  "name": "일반사용자",
  "email": "user01@test.com",
  "password": "Password123!",
  "position": "사원"
}
```

---

## 3) 초기 ADMIN(관리소장) 권한 부여

현재 권한 변경 API는 ADMIN만 가능하므로, 첫 관리자 1명은 DB에서 직접 승격해야 합니다.

MariaDB 접속:

```powershell
mariadb -h 127.0.0.1 -P 3306 -u lab -p -D mini_erp
```

SQL 실행:

```sql
UPDATE users
SET user_role = 'ADMIN'
WHERE login_id = 'manager01';

SELECT id, login_id, user_role FROM users;
```

---

## 4) 로그인 및 토큰 저장

### 4-1) 관리소장 로그인
`POST {{baseUrl}}/api/v1/auth/login`

```json
{
  "id": "manager01",
  "password": "Password123!"
}
```

응답의 `data.accessToken`을 `managerToken`에 저장.

### 4-2) 팀장 로그인
`POST {{baseUrl}}/api/v1/auth/login`

```json
{
  "id": "leader01",
  "password": "Password123!"
}
```

응답 토큰을 `leaderToken`에 저장.

### 4-3) 일반 사용자 로그인
`POST {{baseUrl}}/api/v1/auth/login`

```json
{
  "id": "user01",
  "password": "Password123!"
}
```

응답 토큰을 `userToken`에 저장.

### 4-4) 아이디 찾기
`POST {{baseUrl}}/api/v1/auth/find-id/request`

```json
{
  "name": "일반사용자",
  "email": "user01@test.com"
}
```

기대:
- `success: true`
- `data.id`에 `user01` 반환

### 4-5) 비밀번호 재설정 1단계 (인증번호 발송)
`POST {{baseUrl}}/api/v1/auth/password/reset/request`

```json
{
  "email": "user01@test.com"
}
```

기대:
- 성공 응답 반환
- 메일(또는 콘솔 로그)로 인증번호 확인 후 `verificationCode` 변수에 저장

### 4-6) 비밀번호 재설정 2단계 (인증번호 검증)
`POST {{baseUrl}}/api/v1/auth/password/reset/verify`

```json
{
  "email": "user01@test.com",
  "verificationCode": "{{verificationCode}}"
}
```

기대:
- `data.resetProof` 반환
- 반환값을 `resetProof` 변수에 저장

### 4-7) 비밀번호 재설정 3단계 (새 비밀번호 설정)
`POST {{baseUrl}}/api/v1/auth/password/reset/confirm`

```json
{
  "resetProof": "{{resetProof}}",
  "newPassword": "NewPassword123!",
  "newPasswordConfirm": "NewPassword123!"
}
```

기대:
- 성공 응답 반환
- 이후 로그인은 새 비밀번호(`NewPassword123!`)로 가능

---

## 5) 팀장 권한 부여 (관리소장이 실행)

`PATCH {{baseUrl}}/api/v1/users/{leaderUserId}/role`

Headers:
- `Authorization: Bearer {{managerToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "role": "TEAM_LEADER"
}
```

`{leaderUserId}`는 users 테이블 조회로 확인.

---

## 6) 연차 테스트

## 6-1) 일반 사용자 연차 신청
`POST {{baseUrl}}/api/v1/leave`

Headers:
- `Authorization: Bearer {{userToken}}`
- `Content-Type: application/json`

```json
{
  "appType": "ANNUAL",
  "startDate": "2026-04-06",
  "endDate": "2026-04-06"
}
```

응답 `data.appId`를 `leaveId`에 저장.

### 6-2) 주말 포함 신청 거부 확인 (422)
`POST {{baseUrl}}/api/v1/leave`

```json
{
  "appType": "ANNUAL",
  "startDate": "2026-04-11",
  "endDate": "2026-04-12"
}
```

기대: `LEAVE_DATE_NOT_WORKING_DAY`

### 6-3) 공휴일 포함 신청 거부 확인 (422)
공휴일 테스트를 위해 서버 설정에 공휴일을 추가합니다.

`src/main/resources/application-local.properties`에 아래 한 줄 추가:

```ini
app.holidays=2026-05-25
```

서버 재시작 후 아래 요청 실행:

`POST {{baseUrl}}/api/v1/leave`

```json
{
  "appType": "ANNUAL",
  "startDate": "2026-05-25",
  "endDate": "2026-05-25"
}
```

기대: `LEAVE_DATE_NOT_WORKING_DAY`

추가 안내:
- 고정 공휴일이 주말과 겹치면 대체공휴일(다음 평일)도 자동 차단됩니다.
- 대체공휴일 테스트가 필요하면 해당 연도 날짜를 확인해 같은 방식으로 요청하세요.

### 6-4) 팀장 승인 시도 (거부 기대)
`PATCH {{baseUrl}}/api/v1/leave/{{leaveId}}/approve`

Headers:
- `Authorization: Bearer {{leaderToken}}`

기대: 권한 거부

### 6-5) 관리소장 승인
`PATCH {{baseUrl}}/api/v1/leave/{{leaveId}}/approve`

Headers:
- `Authorization: Bearer {{managerToken}}`

기대: 성공 + 승인 응답 body

### 6-6) 잔여 연차 조회
`GET {{baseUrl}}/api/v1/leave/balance`

Headers:
- `Authorization: Bearer {{userToken}}`

### 6-7) 연차 정책 조회
`GET {{baseUrl}}/api/v1/leave/policy`

Headers:
- `Authorization: Bearer {{userToken}}`

---

## 7) 특근 테스트

### 7-1) 일반 사용자 특근 신청 (주말만 가능)
`POST {{baseUrl}}/api/v1/overtime`

Headers:
- `Authorization: Bearer {{userToken}}`
- `Content-Type: application/json`

```json
{
  "overtimeDate": "2026-04-12",
  "startTime": "19:00:00",
  "endTime": "21:00:00",
  "reason": "긴급 대응"
}
```

기대:
- 성공 시 `data.id`를 `overtimeId`에 저장

### 7-2) 평일 특근 신청 거부 확인 (400)
`POST {{baseUrl}}/api/v1/overtime`

Headers:
- `Authorization: Bearer {{userToken}}`
- `Content-Type: application/json`

```json
{
  "overtimeDate": "2026-04-13",
  "startTime": "19:00:00",
  "endTime": "21:00:00",
  "reason": "평일 테스트"
}
```

기대: `INVALID_OVERTIME_DATE`

### 7-3) 특근 단건 조회
`GET {{baseUrl}}/api/v1/overtime/{{overtimeId}}`

Headers:
- `Authorization: Bearer {{userToken}}`

기대:
- 본인 신청 건 조회 성공

### 7-4) 특근 목록 조회 (권한별 필터링)
`GET {{baseUrl}}/api/v1/overtime/list`

Headers:
- `Authorization: Bearer {{userToken}}`

기대:
- USER: 본인 내역만 조회
- TEAM_LEADER/ADMIN: 전체 내역 조회

### 7-5) 팀장 승인 시도
`PATCH {{baseUrl}}/api/v1/overtime/{{overtimeId}}/approve`

Headers:
- `Authorization: Bearer {{leaderToken}}`

기대:
- 신청자가 USER면 승인 가능
- 신청자가 TEAM_LEADER면 권한 거부

### 7-6) 관리소장 승인
`PATCH {{baseUrl}}/api/v1/overtime/{{overtimeId}}/approve`

Headers:
- `Authorization: Bearer {{managerToken}}`

기대:
- 성공 + 승인 응답 body

### 7-7) 관리소장 신청 건 셀프 승인 정책 확인 (선택)
관리소장 토큰으로 특근 신청 후 같은 토큰으로 승인 호출

기대:
- 정책상 본인 셀프 결재 허용

### 7-8) 기존 `/my` API (호환)
`GET {{baseUrl}}/api/v1/overtime/my`

Headers:
- `Authorization: Bearer {{userToken}}`

기대:
- `/list`와 동일한 권한 필터 결과 반환

---

## 8) 근태 테스트

### 8-1) 출근 기록
`POST {{baseUrl}}/api/v1/attendance/check-in`

Headers:
- `Authorization: Bearer {{userToken}}`
- `Content-Type: application/json`

```json
{
  "workDate": "2026-04-13",
  "clockInTime": "09:05:00"
}
```

### 8-2) 퇴근 기록
`PATCH {{baseUrl}}/api/v1/attendance/check-out?workDate=2026-04-13`

Headers:
- `Authorization: Bearer {{userToken}}`
- `Content-Type: application/json`

```json
{
  "clockOutTime": "18:10:00"
}
```

### 8-3) 출퇴근 수정
`PUT {{baseUrl}}/api/v1/attendance?workDate=2026-04-13`

Headers:
- `Authorization: Bearer {{userToken}}`
- `Content-Type: application/json`

```json
{
  "clockInTime": "09:00:00",
  "clockOutTime": "18:00:00"
}
```

### 8-4) 월별 근태 요약
`GET {{baseUrl}}/api/v1/attendance/summary?month=2026-04`

Headers:
- `Authorization: Bearer {{userToken}}`

기대:
- `workDaysCount`, `clockInTimes`, `clockOutTimes`, `leaveUsedCount` 확인

## 8-2) 캘린더 통합 조회

`GET {{baseUrl}}/api/v1/calendar/events?year=2026&month=4`

Headers:
- `Authorization: Bearer {{userToken}}`

기대:
- 승인된 연차는 `type: LEAVE`
- 승인된 특근은 `type: OVERTIME`

---

## 9) 문제 발생 시 빠른 확인

1. `401 Unauthorized`: 토큰 누락/만료 확인
2. `403 Forbidden`: 현재 역할로 불가한 작업
3. `422`: 주말/공휴일 포함 연차 신청 또는 이미 처리된 결재건
4. `400`: 평일 특근 신청(`INVALID_OVERTIME_DATE`) 또는 잘못된 입력값
5. 비밀번호 재설정: `verificationCode`/`resetProof` 값과 만료 시간 확인
6. 로그인 직후 권한이 반영 안 되면 서버 재시작 후 재로그인
