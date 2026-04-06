# REST API 설계서

## 문서 정보
- **프로젝트명**: 사내 프로젝트 관리 및 결재 통합 시스템사내 프로젝트/업무 트래킹과 연차/휴가 전자결재를 하나로 합친 그룹웨어
- **작성자**: [팀명/김보민]
- **작성일**: [2026-04-03]
- **버전**: [v1.0]
- **검토자**: [김보민, 김민희, 이새연]
- **API 버전**: v1
- **Base URL**: http://localhost:8080/api/v1

---

## 1. API 설계 개요

### 1.1 설계 목적
RESTful 원칙에 따라 클라이언트-서버 간 통신 규격을 정의하여 일관되고 확장 가능한 API를 제공한다.

### 1.2 설계 원칙
- RESTful 아키텍처: HTTP 메서드와 상태 코드를 올바르게 사용한다.
- 일관성: 모든 API 엔드포인트에서 동일한 규칙을 적용한다.
- 버전 관리: URL 경로를 통해 버전을 구분한다.
- 보안: JWT Access Token 기반 인증(Access Token only)으로 운영한다.
- 문서화: 요청/응답 스펙을 명확하게 제공한다.

### 1.3 기술 스택
- Spring Boot 3.x
- JWT Access Token only
- JSON
- OpenAPI 3.0 (Swagger)

---

## 2. API 공통 규칙

### 2.1 공통 요청 헤더
```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {JWT_TOKEN}
```

### 2.2 공통 성공 응답
```json
{
  "success": true,
  "data": {},
  "message": "요청이 성공적으로 처리되었습니다",
  "timestamp": "2026-04-03T10:30:00"
}
```

### 2.3 공통 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "INVALID_INPUT_VALUE",
    "message": "잘못된 요청 값입니다."
  },
  "timestamp": "2026-04-03T10:30:00"
}
```

### 2.4 주요 HTTP 상태 코드
| 코드 | 설명 |
|------|------|
| 200 | OK |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict |
| 422 | Unprocessable Entity |
| 500 | Internal Server Error |

---

## 3. 인증 및 권한 관리

### 3.1 로그인
- `POST /api/v1/auth/login`
- 요청 필드: `id`, `password`
- 응답: Access Token + 사용자 정보

로그인 응답 예시:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 3,
      "name": "일반사용자",
      "email": "user01@test.com",
      "departmentCode": "03",
      "departmentName": "모바일개발팀",
      "position": "사원",
      "role": "USER"
    }
  },
  "message": "로그인이 성공하였습니다"
}
```

### 3.2 권한 레벨
| 역할 | 권한 |
|------|------|
| USER | 본인 데이터 조회/신청 |
| TEAM_LEADER | 일반 사용자 신청 승인/반려, 프로젝트 관리 |
| ADMIN | 최상위 권한, 전사 관리 |

### 3.3 승인 정책
- USER 신청 건 → TEAM_LEADER만 승인/반려 가능
- TEAM_LEADER 신청 건 → ADMIN만 승인/반려 가능
- ADMIN 신청 건 → 처리 불가

---

## 4. 상세 API 명세

### 4.1 인증/사용자 계정 API
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/find-id/request`
- `POST /api/v1/auth/password/reset/request`
- `POST /api/v1/auth/password/reset/verify`
- `POST /api/v1/auth/password/reset/confirm`

회원가입 요청 예시:
```json
{
  "id": "user01",
  "name": "일반사용자",
  "email": "user01@test.com",
  "password": "Password123!",
  "departmentCode": "01",
  "position": "사원"
}
```

### 4.2 사용자 관리 API
- `GET /api/v1/users`
- `GET /api/v1/users/{userId}`
- `PUT /api/v1/users/{userId}`
- `PATCH /api/v1/users/{userId}/role` (ADMIN only)

사용자 수정 요청 예시:
```json
{
  "name": "일반사용자",
  "departmentCode": "01",
  "position": "사원"
}
```

### 4.3 프로젝트 관리 API
- `POST /api/v1/projects`
- `GET /api/v1/projects`
- `PUT /api/v1/projects/{projectId}`
- `PATCH /api/v1/projects/{projectId}/leader`
- `POST /api/v1/projects/{projectId}/members`
- `DELETE /api/v1/projects/{projectId}/members/{userId}`
- `GET /api/v1/projects/{projectId}/members`
- `GET /api/v1/projects/{projectId}/members/available`
- `GET /api/v1/projects/{projectId}/members/assignable`
- `GET /api/v1/projects/permissions/{userId}`
- `PUT /api/v1/projects/permissions/{userId}`
- `GET /api/v1/projects/leaders`
- `GET /api/v1/projects/{projectId}/progress`

### 4.4 업무(Task) 관리 API
- `POST /api/v1/tasks`
- `GET /api/v1/tasks`
- `GET /api/v1/tasks/{taskId}`
- `PUT /api/v1/tasks/{taskId}`
- `PATCH /api/v1/tasks/{taskId}/status`
- `POST /api/v1/tasks/{taskId}/assignments`
- `GET /api/v1/tasks/{taskId}/assignments`
- `DELETE /api/v1/tasks/{taskId}/assignments/{userId}`
- `GET /api/v1/tasks/recent-assignments`

### 4.5 근태 API
- `POST /api/v1/attendance/check-in`
- `PATCH /api/v1/attendance/check-out?workDate=YYYY-MM-DD`
- `PUT /api/v1/attendance?workDate=YYYY-MM-DD`
- `GET /api/v1/attendance/summary?month=YYYY-MM`

### 4.6 캘린더 API
- `GET /api/v1/calendar/events?year=2026&month=4`

### 4.7 연차 API
- `POST /api/v1/leave`
- `PATCH /api/v1/leave/{requestId}/approve`
- `PATCH /api/v1/leave/{requestId}/reject`
- `PATCH /api/v1/leave/{requestId}/cancel`
- `GET /api/v1/leave/my?includeCancelled=false`
- `GET /api/v1/leave/all?includeCancelled=false`
- `GET /api/v1/leave/balance`
- `GET /api/v1/leave/policy`

연차 신청 규칙:
- 주말/공휴일/대체공휴일 포함 신청 불가
- 같은 기간 또는 겹치는 기간 중복 신청 불가
- 반차는 시작일과 종료일이 같은 하루만 가능
- 반차 선택 시 서버가 종료일을 시작일로 자동 보정
- 신청 취소는 본인 PENDING 건만 가능
- 신청 취소 기능은 `PATCH /api/v1/leave/{requestId}/cancel`

연차 조회 규칙:
- 기본 조회(`includeCancelled=false`)는 취소(CANCELLED) 건 제외
- `includeCancelled=true` 지정 시 취소 건 포함

### 4.8 특근 API
- `POST /api/v1/overtime`
- `PATCH /api/v1/overtime/{id}/approve`
- `PATCH /api/v1/overtime/{id}/reject`
- `PATCH /api/v1/overtime/{id}/cancel`
- `GET /api/v1/overtime/{id}`
- `GET /api/v1/overtime/list?includeCancelled=false` (USER 전용)
- `GET /api/v1/overtime/all?includeCancelled=false` (TEAM_LEADER/ADMIN 전용)
- `GET /api/v1/overtime/my?includeCancelled=false` (호환용, Deprecated)

특근 규칙:
- 평일 특근 신청 불가
- 과거 날짜 특근 신청 불가
- 시작시간이 종료시간보다 늦으면 안 됨
- 신청 취소는 본인 PENDING 건만 가능 (`PATCH /api/v1/overtime/{id}/cancel`)

특근 조회 규칙:
- 기본 조회(`includeCancelled=false`)는 취소(CANCELLED) 건 제외
- `includeCancelled=true` 지정 시 취소 건 포함

### 4.9 대시보드 API
- `GET /api/v1/dashboard/progress`
- `GET /api/v1/dashboard/admin-summary`
- `GET /api/v1/dashboard/projects`

---

## 5. 에러 코드 및 처리

| 코드 | 설명 |
|------|------|
| INVALID_INPUT_VALUE | 입력값 검증 실패 |
| INVALID_CREDENTIALS | 로그인 정보 오류 |
| UNAUTHORIZED | 인증 정보 없음/유효하지 않음 |
| ACCESS_DENIED | 권한 없음 |
| RESOURCE_NOT_FOUND | 리소스 없음 |
| DUPLICATE_RESOURCE | 중복 생성/신청 |
| LEAVE_DATE_NOT_WORKING_DAY | 주말/공휴일 연차 신청 |
| LEAVE_ALREADY_PROCESSED | 이미 처리된 연차 |
| REJECT_REASON_REQUIRED | 반려 사유 누락 |
| INVALID_OVERTIME_DATE | 평일 특근 신청 |
| INVALID_OVERTIME_TIME | 특근 시간 역전 |
| OVERTIME_NOT_FOUND | 특근 신청 없음 |

---

## 6. API 문서화
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI 3.0 사용

---

## 7. 체크리스트 및 품질 관리
- RESTful URL/메서드 일관성
- 응답 포맷 표준화
- 인증/인가 적용
- 페이지네이션 적용
- 에러 응답 일관성

---

## 8. 마무리
현재 구현된 핵심 범위:
- 회원가입/로그인
- 아이디/비밀번호 찾기
- 사용자 관리
- 프로젝트/업무 관리
- 연차/특근/근태
- 캘린더 조회
- 권한 관리
- CORS 설정
- Flyway 마이그레이션
