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
> RESTful 원칙에 따라 클라이언트-서버 간 통신 규격을 정의하여 일관되고 확장 가능한 API를 제공

### 1.2 설계 원칙
- **RESTful 아키텍처**: HTTP 메서드와 상태 코드의 올바른 사용
- **일관성**: 모든 API 엔드포인트에서 동일한 규칙 적용
- **버전 관리**: URL 경로를 통한 버전 구분
- **보안**: JWT Access Token 기반 인증 (Access Token only)
- **성능**: 페이지네이션, 캐싱 가능 구조 고려
- **문서화**: 명확한 요청/응답 스펙 제공

### 1.3 기술 스택
- **프레임워크**: Spring Boot 3.x
- **인증**: JWT (Access Token only)
- **직렬화**: JSON
- **API 문서**: OpenAPI 3.0 (Swagger)

---

## 2. API 공통 규칙

### 2.1 URL 설계 규칙
| 규칙 | 설명 | 좋은 예 | 나쁜 예 |
|------|------|---------|---------|
| **명사 사용** | 동사가 아닌 명사로 리소스 표현 | `/api/v1/tasks` | `/api/v1/getTasks` |
| **복수형 사용** | 컬렉션은 복수형으로 표현 | `/api/v1/users` | `/api/v1/user` |
| **계층 구조** | 리소스 간 관계를 URL로 표현 | `/api/v1/projects/1/members` | `/api/v1/getProjectMembers` |
| **소문자 사용** | URL은 소문자와 하이픈 사용 | `/api/v1/password/reset/request` | `/api/v1/PasswordReset` |
| **동작 표현** | HTTP 메서드로 동작 구분 | `POST /api/v1/leave` | `/api/v1/createLeave` |

### 2.2 HTTP 메서드 사용 규칙
| 메서드 | 용도 | 멱등성 | 안전성 | 예시 |
|--------|------|--------|--------|------|
| **GET** | 리소스 조회 | ✅ | ✅ | `GET /api/v1/users` |
| **POST** | 리소스 생성 | ❌ | ❌ | `POST /api/v1/tasks` |
| **PUT** | 리소스 전체 수정 | ✅ | ❌ | `PUT /api/v1/users/3` |
| **PATCH** | 리소스 부분 수정 | ❌ | ❌ | `PATCH /api/v1/leave/10/approve` |
| **DELETE** | 리소스 삭제 | ✅ | ❌ | `DELETE /api/v1/tasks/4/assignments/3` |

### 2.3 HTTP 상태 코드 가이드
| 코드 | 상태 | 설명 | 사용 예시 |
|------|------|------|----------|
| **200** | OK | 성공 (데이터 포함) | GET, PATCH, PUT 성공 |
| **201** | Created | 리소스 생성 성공 | 회원가입, 신청 생성 |
| **204** | No Content | 성공 (응답 데이터 없음) | (현재 구현에서 미사용, 확장 가능) |
| **400** | Bad Request | 잘못된 요청 | 입력 검증 실패 |
| **401** | Unauthorized | 인증 필요/실패 | 토큰 없음, 유효하지 않은 토큰 |
| **403** | Forbidden | 권한 없음 | 승인 권한 없음 |
| **404** | Not Found | 리소스 없음 | 사용자/프로젝트/업무 없음 |
| **409** | Conflict | 충돌 | 중복 신청, 중복 배정 |
| **422** | Unprocessable Entity | 의미상 오류 | 주말/공휴일 연차 신청 |
| **500** | Internal Server Error | 서버 오류 | 예기치 못한 오류 |

### 2.4 공통 요청 헤더
```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {JWT_TOKEN}
```

### 2.5 공통 응답 형식
#### 성공 응답 (단일 객체)
```json
{
  "success": true,
  "data": {},
  "message": "요청이 성공적으로 처리되었습니다",
  "timestamp": "2026-04-03T10:30:00"
}
```

#### 성공 응답 (목록/페이지네이션)
```json
{
  "success": true,
  "data": {
    "content": [],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8,
      "first": true,
      "last": false,
      "numberOfElements": 20
    }
  },
  "message": "목록 조회가 완료되었습니다",
  "timestamp": "2026-04-03T10:30:00"
}
```

#### 에러 응답
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

---

## 3. 인증 및 권한 관리

### 3.1 JWT 토큰 기반 인증

#### 3.1.1 로그인 API
```yaml
POST /api/v1/auth/login
Content-Type: application/json

Request Body:
{
  "id": "user01",
  "password": "Password123!"
}

Response (200 OK):
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

Response (401 Unauthorized):
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "아이디 또는 비밀번호가 올바르지 않습니다."
  }
}
```

#### 3.1.2 로그아웃 API
- Access Token only 정책으로 서버 별도 로그아웃 엔드포인트는 제공하지 않음
- 클라이언트에서 토큰 삭제로 로그아웃 처리

### 3.2 권한 레벨 정의
| 역할 | 권한 | 설명 |
|------|------|------|
| **USER** | 일반 사용자 | 본인 데이터 조회/신청 (연차, 특근, 근태, 본인 사용자 정보) |
| **TEAM_LEADER** | 팀장 | 일반 사용자 신청 승인/반려, 프로젝트/업무 관리 범위 권한 |
| **ADMIN** | 관리자(관리 소장) | 최상위 권한, 사용자 권한 변경, 전사 조회/관리 |

---

## 4. 상세 API 명세

### 4.1 인증/사용자 계정 API

#### 4.1.1 회원가입
```yaml
POST /api/v1/auth/signup
Public API
```

Request Body 예시:
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

#### 4.1.2 로그인
```yaml
POST /api/v1/auth/login
Public API
```

#### 4.1.3 아이디 찾기
```yaml
POST /api/v1/auth/find-id/request
Public API
```

#### 4.1.4 비밀번호 재설정 요청
```yaml
POST /api/v1/auth/password/reset/request
Public API
```

#### 4.1.5 비밀번호 재설정 인증코드 검증
```yaml
POST /api/v1/auth/password/reset/verify
Public API
```

#### 4.1.6 비밀번호 재설정 확정
```yaml
POST /api/v1/auth/password/reset/confirm
Public API
```

### 4.2 사용자 관리 API

#### 4.2.1 사용자 목록 조회
```yaml
GET /api/v1/users
Authorization: Bearer {JWT_TOKEN}
```

#### 4.2.2 사용자 상세 조회
```yaml
GET /api/v1/users/{userId}
Authorization: Bearer {JWT_TOKEN}
```

- Response DTO에는 `departmentCode`, `departmentName`이 포함됩니다.

#### 4.2.3 사용자 정보 수정
```yaml
PUT /api/v1/users/{userId}
Authorization: Bearer {JWT_TOKEN}
```

Request Body 예시:
```json
{
  "name": "일반사용자",
  "departmentCode": "01",
  "position": "사원"
}
```

#### 4.2.4 사용자 권한 변경
```yaml
PATCH /api/v1/users/{userId}/role
Authorization: Bearer {JWT_TOKEN}
Required Role: ADMIN
```

### 4.3 프로젝트 관리 API

#### 4.3.1 프로젝트 생성
```yaml
POST /api/v1/projects
Authorization: Bearer {JWT_TOKEN}
```

#### 4.3.2 프로젝트 목록 조회
```yaml
GET /api/v1/projects
Authorization: Bearer {JWT_TOKEN}
```

#### 4.3.3 프로젝트 수정
```yaml
PUT /api/v1/projects/{projectId}
Authorization: Bearer {JWT_TOKEN}
```

#### 4.3.4 프로젝트 팀장 변경
```yaml
PATCH /api/v1/projects/{projectId}/leader
Authorization: Bearer {JWT_TOKEN}
```

#### 4.3.5 프로젝트 멤버 배정/해제
```yaml
POST /api/v1/projects/{projectId}/members
DELETE /api/v1/projects/{projectId}/members/{userId}
Authorization: Bearer {JWT_TOKEN}
```

#### 4.3.6 프로젝트 멤버 조회/후보 조회
```yaml
GET /api/v1/projects/{projectId}/members
GET /api/v1/projects/{projectId}/members/available
GET /api/v1/projects/{projectId}/members/assignable
Authorization: Bearer {JWT_TOKEN}
```

#### 4.3.7 사용자 프로젝트 권한 조회/저장
```yaml
GET /api/v1/projects/permissions/{userId}
PUT /api/v1/projects/permissions/{userId}
Authorization: Bearer {JWT_TOKEN}
```

#### 4.3.8 팀장 목록/프로젝트 진행률
```yaml
GET /api/v1/projects/leaders
GET /api/v1/projects/{projectId}/progress
Authorization: Bearer {JWT_TOKEN}
```

### 4.4 업무(Task) 관리 API

#### 4.4.1 업무 생성/조회/상세/수정
```yaml
POST /api/v1/tasks
GET /api/v1/tasks
GET /api/v1/tasks/{taskId}
PUT /api/v1/tasks/{taskId}
Authorization: Bearer {JWT_TOKEN}
```

#### 4.4.2 업무 상태 변경
```yaml
PATCH /api/v1/tasks/{taskId}/status
Authorization: Bearer {JWT_TOKEN}
```

#### 4.4.3 업무 담당자 배정 관리
```yaml
POST /api/v1/tasks/{taskId}/assignments
GET /api/v1/tasks/{taskId}/assignments
DELETE /api/v1/tasks/{taskId}/assignments/{userId}
Authorization: Bearer {JWT_TOKEN}
```

#### 4.4.4 최근 업무 배정 이력
```yaml
GET /api/v1/tasks/recent-assignments
Authorization: Bearer {JWT_TOKEN}
```

### 4.5 근태 관리 API

#### 4.5.1 출근
```yaml
POST /api/v1/attendance/check-in
Authorization: Bearer {JWT_TOKEN}
```

#### 4.5.2 퇴근
```yaml
PATCH /api/v1/attendance/check-out?workDate=YYYY-MM-DD
Authorization: Bearer {JWT_TOKEN}
```

#### 4.5.3 근태 기록 수정
```yaml
PUT /api/v1/attendance?workDate=YYYY-MM-DD
Authorization: Bearer {JWT_TOKEN}
```

#### 4.5.4 월별 요약
```yaml
GET /api/v1/attendance/summary?month=YYYY-MM
Authorization: Bearer {JWT_TOKEN}
```

### 4.6 캘린더 API

#### 4.6.1 캘린더 이벤트 조회
```yaml
GET /api/v1/calendar/events?year=2026&month=4
Authorization: Bearer {JWT_TOKEN}
```

### 4.7 연차 API

#### 4.7.1 연차 신청
```yaml
POST /api/v1/leave
Authorization: Bearer {JWT_TOKEN}
```

요청 예시:
```json
{
  "appType": "ANNUAL",
  "startDate": "2026-04-10",
  "endDate": "2026-04-10",
  "requestReason": "개인 일정"
}
```

#### 4.7.2 연차 승인/반려
```yaml
PATCH /api/v1/leave/{requestId}/approve
PATCH /api/v1/leave/{requestId}/reject
Authorization: Bearer {JWT_TOKEN}
```

#### 4.7.3 연차 내역 조회
```yaml
GET /api/v1/leave/my
GET /api/v1/leave/all
Authorization: Bearer {JWT_TOKEN}
```

#### 4.7.4 연차 잔여/정책 조회
```yaml
GET /api/v1/leave/balance
GET /api/v1/leave/policy
Authorization: Bearer {JWT_TOKEN}
```

연차 정책:
- 사원 14일
- 대리 16일
- 과장 17일
- 팀장 18일
- 관리소장 19일

### 4.8 특근 API

#### 4.8.1 특근 신청
```yaml
POST /api/v1/overtime
Authorization: Bearer {JWT_TOKEN}
```

#### 4.8.2 특근 승인/반려
```yaml
PATCH /api/v1/overtime/{id}/approve
PATCH /api/v1/overtime/{id}/reject
Authorization: Bearer {JWT_TOKEN}
```

#### 4.8.3 특근 조회
```yaml
GET /api/v1/overtime/{id}
GET /api/v1/overtime/list
GET /api/v1/overtime/my
Authorization: Bearer {JWT_TOKEN}
```

### 4.9 대시보드 API

#### 4.9.1 대시보드 진행률
```yaml
GET /api/v1/dashboard/progress
Authorization: Bearer {JWT_TOKEN}
```

#### 4.9.2 관리자 요약
```yaml
GET /api/v1/dashboard/admin-summary
Authorization: Bearer {JWT_TOKEN}
```

#### 4.9.3 대시보드 프로젝트 현황
```yaml
GET /api/v1/dashboard/projects
Authorization: Bearer {JWT_TOKEN}
```

---

## 5. 에러 코드 및 처리

### 5.1 표준 에러 코드 정의
| 코드 | HTTP 상태 | 설명 | 해결 방법 |
|------|-----------|------|-----------|
| **INVALID_INPUT_VALUE** | 400 | 입력값 검증 실패 | 요청 데이터 확인 |
| **INVALID_CREDENTIALS** | 401 | 로그인 정보 오류 | 아이디/비밀번호 확인 |
| **UNAUTHORIZED** | 401 | 인증 정보 없음/유효하지 않음 | 토큰 재발급/재로그인 |
| **ACCESS_DENIED** | 403 | 권한 없음 | 권한 확인 |
| **RESOURCE_NOT_FOUND** | 404 | 리소스 없음 | ID/경로 확인 |
| **DUPLICATE_RESOURCE** | 409 | 중복 생성/신청 | 기존 데이터 확인 |
| **INTERNAL_SERVER_ERROR** | 500 | 서버 내부 오류 | 관리자 문의 |

### 5.2 비즈니스 로직 에러 코드
| 코드 | 설명 |
|------|------|
| `LOGIN_ID_ALREADY_EXISTS` | 중복 아이디 |
| `EMAIL_ALREADY_EXISTS` | 중복 이메일 |
| `USER_NOT_FOUND` | 사용자 없음 |
| `PROJECT_NOT_FOUND` | 프로젝트 없음 |
| `TASK_NOT_FOUND` | 업무 없음 |
| `TASK_ACCESS_DENIED` | 배정된 업무가 아님 |
| `DUPLICATE_PROJECT_MEMBER` | 프로젝트 멤버 중복 |
| `DUPLICATE_ASSIGNMENT` | 업무 담당자 중복 |
| `LEAVE_DATE_NOT_WORKING_DAY` | 주말/공휴일 연차 신청 |
| `LEAVE_ALREADY_PROCESSED` | 이미 처리된 연차 |
| `REJECT_REASON_REQUIRED` | 반려 사유 누락 |
| `OVERTIME_NOT_FOUND` | 특근 신청 없음 |
| `INVALID_OVERTIME_DATE` | 평일 특근 신청 |
| `INVALID_OVERTIME_TIME` | 특근 시간 역전 |
| `OVERTIME_APPROVAL_REQUIRED` | 휴일 출근 시 특근 승인 필요 |

---

## 6. API 문서화

### 6.1 OpenAPI 3.0 스펙 예시
```yaml
openapi: 3.0.3
info:
  title: Mini ERP API
  version: 1.0.0
servers:
  - url: http://localhost:8080/api/v1
components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

### 6.2 API 문서 생성 도구
- Swagger UI (`springdoc-openapi`) 사용
- URL: `http://localhost:8080/swagger-ui/index.html`

---

## 7. 체크리스트 및 품질 관리

### 7.1 API 설계 체크리스트
```text
□ RESTful 원칙을 준수하는가?
□ URL 네이밍 규칙이 일관되는가?
□ HTTP 메서드를 올바르게 사용하는가?
□ HTTP 상태 코드를 적절히 사용하는가?
□ 요청/응답 형식이 표준화되어 있는가?
□ 에러 응답이 명확한가?
□ 인증/인가가 엔드포인트에 적용되어 있는가?
```

### 7.2 보안 체크리스트
```text
□ 입력값 검증이 충분한가?
□ JWT 토큰이 안전하게 관리되는가?
□ 권한 체크가 모든 엔드포인트에 적용되는가?
□ 에러 메시지로 내부 정보가 노출되지 않는가?
```

### 7.3 성능 체크리스트
```text
□ N+1 쿼리 문제가 없는가?
□ 인덱스/조회 조건이 적절한가?
□ 목록 조회에 페이지네이션이 적용되어 있는가?
```

---

## 8. 마무리

### 8.1 주요 포인트 요약
1. **RESTful 설계**: HTTP 메서드/상태코드 일관성
2. **응답 표준화**: `ApiResponse` + 통합 예외 처리
3. **권한 체계**: USER/TEAM_LEADER/ADMIN 계층 정책
4. **문서화/운영성**: Swagger + Flyway 기반 변경 이력 관리

### 8.2 추천 도구 및 라이브러리
- **문서화**: Swagger/OpenAPI, Postman
- **마이그레이션**: Flyway
- **테스트**: Spring Boot Test + MockMvc
