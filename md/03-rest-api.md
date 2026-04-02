# REST API 설계서

##  문서 정보
- **프로젝트명**: 사내 프로젝트 트래킹 및 연차 전자결재 그룹웨어
- **작성자**: 팀 작성
- **작성일**: 2026-03-30
- **버전**: v2.1
- **검토자**: 백엔드 개발팀
- **API 버전**: v1
- **Base URL**: https://localhost:8080/api/v1

---

## 1. API 설계 개요

### 1.1 설계 목적
> RESTful 원칙에 따라 클라이언트-서버 간 통신 규격을 정의하여 일관되고 확장 가능한 API를 제공

### 1.2 설계 원칙
- **RESTful 아키텍처**: HTTP 메서드와 상태 코드의 올바른 사용
- **일관성**: 모든 API 엔드포인트에서 동일한 규칙 적용
- **버전 관리**: URL 경로를 통한 버전 구분
- **보안**: JWT Access Token 기반 인증(Access Token only)
- **성능**: 페이지네이션, 캐싱
- **문서화**: 명확한 요청/응답 스펙 제공

### 1.3 기술 스택
- **프레임워크**: Spring Boot 3.4.6
- **인증**: JWT Access Token only
- **직렬화**: JSON
- **API 문서**: OpenAPI 3.0 (Swagger)

---

## 2. API 공통 규칙

### 2.1 URL 설계 규칙
| 규칙 | 설명 | 좋은 예 | 나쁜 예 |
|------|------|---------|---------|
| **명사 사용** | 동사가 아닌 명사로 리소스 표현 | `/api/v1/tasks` | `/api/v1/getTasks` |
| **복수형 사용** | 컬렉션은 복수형으로 표현 | `/api/v1/task-assignments` | `/api/v1/task-assignment` |
| **계층 구조** | 리소스 간 관계를 URL로 표현 | `/api/v1/tasks/1/assignments` | `/api/v1/getTaskAssignments` |
| **소문자 사용** | URL은 소문자와 하이픈 사용 | `/api/v1/leave-requests` | `/api/v1/LeaveRequests` |
| **동작 표현** | HTTP 메서드로 동작 구분 | `POST /api/v1/tasks` | `/api/v1/createTask` |

### 2.2 HTTP 메서드 사용 규칙
| 메서드 | 용도 | 멱등성 | 안전성 | 예시 |
|--------|------|--------|--------|------|
| **GET** | 리소스 조회 | ✅ | ✅ | `GET /api/v1/tasks` |
| **POST** | 리소스 생성 | ❌ | ❌ | `POST /api/v1/tasks` |
| **PUT** | 리소스 전체 수정 | ✅ | ❌ | `PUT /api/v1/users/1` |
| **PATCH** | 리소스 부분 수정 | ❌ | ❌ | `PATCH /api/v1/tasks/1/status` |
| **DELETE** | 리소스 삭제 | ✅ | ❌ | `DELETE /api/v1/tasks/1/assignments/3` |

### 2.3 HTTP 상태 코드 가이드
| 코드 | 상태 | 설명 | 사용 예시 |
|------|------|------|----------|
| **200** | OK | 성공 (데이터 포함) | GET 요청 성공 |
| **201** | Created | 리소스 생성 성공 | POST 요청 성공 |
| **204** | No Content | 성공 (응답 데이터 없음) | 삭제/해제 성공 |
| **400** | Bad Request | 잘못된 요청 | 검증 실패 |
| **401** | Unauthorized | 인증 필요 | 토큰 없음/만료 |
| **403** | Forbidden | 권한 없음 | 접근 거부 |
| **404** | Not Found | 리소스 없음 | 존재하지 않는 ID |
| **409** | Conflict | 충돌 | 중복 업무 담당자 배정 |
| **422** | Unprocessable Entity | 의미상 오류 | 비즈니스 규칙 위반 |
| **500** | Internal Server Error | 서버 오류 | 예기치 못한 오류 |

### 2.4 공통 요청 헤더
```
Content-Type: application/json
Accept: application/json
Authorization: Bearer {ACCESS_TOKEN}
```

### 2.5 공통 응답 형식
#### 성공 응답 (단일 객체)
```json
{
  "success": true,
  "data": {
    "id": 1
  },
  "message": "요청이 성공적으로 처리되었습니다",
  "timestamp": "2026-03-28T10:30:00Z"
}
```

#### 성공 응답 (목록/페이지네이션)
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "ERP 재구축"
      }
    ],
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
  "timestamp": "2026-03-28T10:30:00Z"
}
```

#### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "INVALID_INPUT_VALUE",
    "message": "입력값이 유효하지 않습니다"
  },
  "timestamp": "2026-03-28T10:30:00Z"
}
```

---

## 3. 인증 및 권한 관리

### 3.1 Access Token 기반 인증

#### 3.1.1 로그인 API
```yaml
POST /api/v1/auth/login
Content-Type: application/json

Request Body:
{
  "id": "testuser01",
  "password": "Password123!"
}

Response (200 OK):
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "name": "김철수",
      "email": "user@company.com",
      "position": "사원",
      "role": "USER"
    }
  },
  "message": "로그인이 성공하였습니다",
  "timestamp": "2026-03-31T10:03:18.8324988"
}

Response (401 Unauthorized):
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "아이디 또는 비밀번호가 올바르지 않습니다"
  },
  "timestamp": "2026-03-31T10:03:18.8324988"
}
```

#### 3.1.2 로그아웃 처리 정책
- Access Token only 정책으로 서버 측 별도 로그아웃 엔드포인트를 두지 않습니다.
- 클라이언트는 토큰 제거로 로그아웃을 처리하고, 만료/서명 검증 실패 토큰은 서버가 거부합니다.

### 3.2 권한 레벨 정의
| 역할 | 권한 | 설명 | 주요 기능 |
|------|------|------|----------|
| **USER** | 일반 사용자 | 본인에게 할당된 데이터만 조회/신청 | • 본인 투입 프로젝트/배정 Task 조회<br/>• Task 상태 변경<br/>• 본인 연차/특근 신청/조회 |
| **TEAM_LEADER** | 팀장 | 일반 사용자 결재, 전체 목록 조회, 업무 배정 권한 | • 일반 사용자의 연차/특근 승인/반려<br/>• 전체 연차/특근 목록 조회<br/>• 업무 배정 가능 |
| **ADMIN** | 관리 소장 | 최상위 관리자, 모든 권한 | • 프로젝트 생성/관리<br/>• 사용자 권한 변경<br/>• 팀장/관리소장의 연차/특근 승인<br/>• 전체 사용자/프로젝트 관리 |

---

## 4. 상세 API 명세

### 4.1 인증/사용자 관리 API

#### 4.1.1 회원가입
- Request DTO: `SignupRequestDto`
- Response DTO: `UserResponseDto`
```yaml
POST /api/v1/auth/signup
Content-Type: application/json
Public API

Request Body:
{
  "id": "user01",
  "name": "일반사용자",
  "email": "user01@test.com",
  "password": "Password123!",
  "position": "사원"
}

Response (201 Created):
{
  "success": true,
  "data": {
    "id": 1,
    "name": "일반사용자",
    "email": "user01@test.com",
    "position": "사원",
    "role": "USER"
  },
  "message": "회원가입이 완료되었습니다",
  "timestamp": "..."
}
```

#### 4.1.2 로그인
- Request DTO: `LoginRequestDto`
- Response DTO: `LoginResponseDto`
```yaml
POST /api/v1/auth/login
Content-Type: application/json
Public API

Request Body:
{
  "id": "user01",
  "password": "Password123!"
}

Response (200 OK):
{
  "success": true,
  "data": {
    "accessToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "name": "일반사용자",
      "email": "user01@test.com",
      "position": "사원",
      "role": "USER"
    }
  },
  "message": "로그인이 성공하였습니다",
  "timestamp": "..."
}
```

#### 4.1.3 아이디 찾기
- Request DTO: `FindIdRequestDto`
- Response DTO: `FindIdResponseDto`
```yaml
POST /api/v1/auth/find-id/request
Content-Type: application/json
Public API

Request Body:
{
  "name": "일반사용자",
  "email": "user01@test.com"
}

Response (200 OK):
{
  "success": true,
  "data": { "id": "user01" },
  "message": "아이디를 찾았습니다",
  "timestamp": "..."
}
```

#### 4.1.4 비밀번호 찾기(3단계)
- Request DTO
  - 요청: `PasswordResetRequestDto`
  - 검증: `PasswordResetVerifyDto`
  - 확정: `PasswordResetConfirmDto`
- Response DTO
  - 요청: `PasswordResetRequestResponseDto`
  - 검증: `PasswordResetVerifyResponseDto`
  - 확정: `PasswordResetConfirmResponseDto`
```yaml
POST /api/v1/auth/password/reset/request
POST /api/v1/auth/password/reset/verify
POST /api/v1/auth/password/reset/confirm
```

#### 4.1.5 사용자 목록 조회
- Response DTO: `UserListResponseDto`
```yaml
GET /api/v1/users
Authorization: Bearer {ACCESS_TOKEN}
```
- USER: 본인만 조회
- TEAM_LEADER/ADMIN: 필터 기반 목록 조회

#### 4.1.6 사용자 상세 조회
- Response DTO: `UserResponseDto`
```yaml
GET /api/v1/users/{userId}
Authorization: Bearer {ACCESS_TOKEN}
Required Role: 본인 또는 ADMIN
```

#### 4.1.7 사용자 정보 수정
- Request DTO: `UserUpdateRequestDto`
- Response DTO: `UserResponseDto`
```yaml
PUT /api/v1/users/{userId}
Authorization: Bearer {ACCESS_TOKEN}
Required Role: 본인 또는 ADMIN
```

#### 4.1.8 사용자 권한 변경
- Request DTO: `UserRoleUpdateRequestDto`
- Response DTO: `UserRoleUpdateResponseDto`
```yaml
PATCH /api/v1/users/{userId}/role
Authorization: Bearer {ACCESS_TOKEN}
Required Role: ADMIN(관리 소장)
```

### 4.2 프로젝트/업무/업무할당 관리 API

#### 4.2.1 프로젝트 생성
```yaml
POST /api/v1/projects
Content-Type: application/json
Authorization: Bearer {ACCESS_TOKEN}
Required Role: ADMIN

Request Body:
{
  "title": "ERP 재구축",
  "content": "사내 업무 시스템 고도화",
  "projectType": "ERP",
  "startDate": "2026-03-28",
  "endDate": "2026-04-30"
}

Response (201 Created):
{
  "success": true,
  "data": {
    "projectId": 101,
    "title": "ERP 재구축",
    "projectType": "ERP",
    "status": "READY"
  },
  "message": "프로젝트가 생성되었습니다"
}
```

#### 4.2.2 프로젝트 목록 조회
```yaml
GET /api/v1/projects
Authorization: Bearer {ACCESS_TOKEN}
Required Role: USER, ADMIN

Query Parameters:
- page: integer (default: 0)
- size: integer (default: 20)
- status: string (READY, PROGRESS, DONE, HOLD)
- keyword: string (제목 검색)

Response (200 OK):
{
  "success": true,
  "data": {
    "content": [
      {
        "projectId": 101,
        "title": "ERP 재구축",
        "projectType": "ERP",
        "status": "PROGRESS",
        "startDate": "2026-03-28",
        "endDate": "2026-04-30"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 10,
      "totalPages": 1
    }
  }
}
```

#### 4.2.3 Task 생성
```yaml
POST /api/v1/tasks
Content-Type: application/json
Authorization: Bearer {ACCESS_TOKEN}
Required Role: ADMIN

Request Body:
{
  "projectId": 101,
  "taskTitle": "내 업무 화면 구현",
  "taskContent": "React 페이지 및 API 연동",
  "startDate": "2026-03-28",
  "endDate": "2026-03-31",
  "taskState": "TODO",
  "taskNo": "TASK-2026-001",
  "assigneeIds": [10, 11]
}

Response (201 Created):
{
  "success": true,
  "data": {
    "id": 5001,
    "projectId": 101,
    "taskTitle": "내 업무 화면 구현",
    "taskState": "TODO",
    "taskNo": "TASK-2026-001",
    "assignees": [
      { "id": 10, "name": "박개발" },
      { "id": 11, "name": "최개발" }
    ]
  },
  "message": "Task가 생성되었습니다"
}
```

#### 4.2.4 Task 목록 조회
```yaml
GET /api/v1/tasks
Authorization: Bearer {ACCESS_TOKEN}
Required Role: USER, ADMIN

Query Parameters:
- page: integer (default: 0)
- size: integer (default: 20)
- projectId: integer
- taskState: string (TODO, DOING, DONE)
- assignedToMe: boolean (default: true for USER)

Response (200 OK):
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 5001,
        "projectId": 101,
        "taskTitle": "내 업무 화면 구현",
        "taskState": "DOING",
        "startDate": "2026-03-28",
        "endDate": "2026-03-31",
        "taskNo": "TASK-2026-001",
        "assignees": [
          {
            "id": 10,
            "name": "박개발"
          }
        ]
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 25,
      "totalPages": 2
    }
  }
}
```

#### 4.2.5 Task 상세 조회
```yaml
GET /api/v1/tasks/{taskId}
Authorization: Bearer {ACCESS_TOKEN}
Required Role: USER, ADMIN

Path Parameters:
- taskId: integer (required)

Response (200 OK):
{
  "success": true,
  "data": {
    "id": 5001,
    "projectId": 101,
    "taskTitle": "내 업무 화면 구현",
    "taskContent": "React 페이지 및 API 연동",
    "taskState": "DOING",
    "startDate": "2026-03-28",
    "endDate": "2026-03-31",
    "taskNo": "TASK-2026-001",
    "assignees": [
      {
        "id": 10,
        "name": "박개발"
      }
    ]
  }
}
```

#### 4.2.6 Task 상태 변경
```yaml
PATCH /api/v1/tasks/{taskId}/status
Content-Type: application/json
Authorization: Bearer {ACCESS_TOKEN}
Required Role: USER(배정된 Task), ADMIN

Request Body:
{
  "taskState": "DONE"
}

Response (200 OK):
{
  "success": true,
  "data": {
    "id": 5001,
    "taskState": "DONE",
    "updatedAt": "2026-03-28T16:30:00Z"
  },
  "message": "Task 상태가 변경되었습니다"
}

Response (403 Forbidden):
{
  "success": false,
  "error": {
    "code": "TASK_ACCESS_DENIED",
    "message": "배정된 Task만 상태 변경할 수 있습니다"
  }
}
```

#### 4.2.7 Task 담당자 배정 (task_assignments 생성)
```yaml
POST /api/v1/tasks/{taskId}/assignments
Content-Type: application/json
Authorization: Bearer {ACCESS_TOKEN}
Required Role: ADMIN

Path Parameters:
- taskId: integer (required)

Request Body:
{
  "userId": 10
}

Response (201 Created):
{
  "success": true,
  "data": {
    "id": 7001,
    "taskId": 5001,
    "userId": 10
  },
  "message": "업무 담당자가 배정되었습니다"
}

Response (409 Conflict):
{
  "success": false,
  "error": {
    "code": "DUPLICATE_TASK_ASSIGNMENT",
    "message": "이미 배정된 담당자입니다"
  }
}
```

#### 4.2.8 Task 담당자 목록 조회
```yaml
GET /api/v1/tasks/{taskId}/assignments
Authorization: Bearer {ACCESS_TOKEN}
Required Role: USER, ADMIN

Response (200 OK):
{
  "success": true,
  "data": [
    {
      "id": 7001,
      "taskId": 5001,
      "user": {
        "id": 10,
        "name": "박개발",
        "email": "park@company.com"
      }
    }
  ]
}
```

#### 4.2.9 Task 담당자 배정 해제
```yaml
DELETE /api/v1/tasks/{taskId}/assignments/{userId}
Authorization: Bearer {ACCESS_TOKEN}
Required Role: ADMIN

Response (204 No Content)
```

#### 4.2.10 프로젝트 진행률 조회
```yaml
GET /api/v1/projects/{projectId}/progress
Authorization: Bearer {ACCESS_TOKEN}
Required Role: USER, ADMIN

Response (200 OK):
{
  "success": true,
  "data": {
    "projectId": 101,
    "totalTasks": 20,
    "doneTasks": 8,
    "progressRate": 40
  }
}
```

#### 4.2.11 프로젝트 팀원 배정
```yaml
POST /api/v1/projects/{projectId}/members
Content-Type: application/json
Authorization: Bearer {ACCESS_TOKEN}
Required Role: ADMIN

Path Parameters:
- projectId: integer (required)

Request Body:
{
  "userId": 10,
  "projectRole": "MEMBER"
}

Response (201 Created):
{
  "success": true,
  "data": {
    "id": 3001,
    "projectId": 101,
    "userId": 10,
    "projectRole": "MEMBER"
  },
  "message": "프로젝트 팀원이 배정되었습니다"
}

Response (409 Conflict):
{
  "success": false,
  "error": {
    "code": "DUPLICATE_PROJECT_MEMBER",
    "message": "이미 프로젝트에 배정된 팀원입니다"
  }
}
```

#### 4.2.12 프로젝트 팀원 배정 해제
```yaml
DELETE /api/v1/projects/{projectId}/members/{userId}
Authorization: Bearer {ACCESS_TOKEN}
Required Role: ADMIN

Response (204 No Content)
```

### 4.3 근태/캘린더 API

#### 4.3.1 출근 체크인
- Request DTO: `CheckInRequestDto`
- Response DTO: `CheckInResponseDto`
```yaml
POST /api/v1/attendance/check-in
Authorization: Bearer {ACCESS_TOKEN}

Request Body:
{
  "workDate": "2026-04-13",
  "clockInTime": "09:05:00"
}
```

#### 4.3.2 퇴근 체크아웃
- Request DTO: `CheckOutRequestDto`
- Response DTO: `CheckOutResponseDto`
```yaml
PATCH /api/v1/attendance/check-out?workDate=2026-04-13
Authorization: Bearer {ACCESS_TOKEN}

Request Body:
{
  "clockOutTime": "18:10:00"
}
```

#### 4.3.3 출퇴근 기록 수정
- Request DTO: `AttendanceUpdateRequestDto`
- Response DTO: `AttendanceUpdateResponseDto`
```yaml
PUT /api/v1/attendance?workDate=2026-04-13
Authorization: Bearer {ACCESS_TOKEN}

Request Body:
{
  "clockInTime": "09:00:00",
  "clockOutTime": "18:00:00"
}
```

#### 4.3.4 근태 요약 조회
- Response DTO: `AttendanceSummaryDto`
```yaml
GET /api/v1/attendance/summary?month=2026-04
Authorization: Bearer {ACCESS_TOKEN}
```

#### 4.3.5 캘린더 통합 조회(개인)
- Response DTO: `CalendarEventResponseDto`
```yaml
GET /api/v1/calendar/events?year=2026&month=4
Authorization: Bearer {ACCESS_TOKEN}
```
- 승인된 연차/특근 통합 조회
- 월 경계를 걸치는 연차도 포함

### 4.4 연차 관리 API

#### 4.4.1 연차 신청
- Request DTO: `LeaveRequestCreateDto`
- Response DTO: `LeaveRequestResponseDto`
```yaml
POST /api/v1/leave
Authorization: Bearer {ACCESS_TOKEN}

Request Body:
{
  "appType": "ANNUAL",
  "startDate": "2026-04-06",
  "endDate": "2026-04-06"
}
```

Validation Rules:
- 신청 기간에 주말/공휴일/대체공휴일 포함 시 거부
- 에러 코드: `LEAVE_DATE_NOT_WORKING_DAY` (422)

#### 4.4.2 연차 승인
- Response DTO: `LeaveRequestResponseDto`
```yaml
PATCH /api/v1/leave/{requestId}/approve
Authorization: Bearer {ACCESS_TOKEN}
```

#### 4.4.3 연차 반려
- Request DTO: `RejectRequestDto`
- Response DTO: `LeaveRequestResponseDto`
```yaml
PATCH /api/v1/leave/{requestId}/reject
Authorization: Bearer {ACCESS_TOKEN}
```

#### 4.4.4 내 연차 신청 내역 조회
- Response DTO: `List<LeaveRequestResponseDto>`
```yaml
GET /api/v1/leave/my
Authorization: Bearer {ACCESS_TOKEN}
```

#### 4.4.5 연차 신청 내역 조회(권한 필터)
- Response DTO: `List<LeaveRequestResponseDto>`
```yaml
GET /api/v1/leave/all
Authorization: Bearer {ACCESS_TOKEN}
```
- USER: 본인 내역만
- TEAM_LEADER/ADMIN: 전체 내역

#### 4.4.6 잔여 연차 조회
- Response DTO: `LeaveBalanceResponseDto`
```yaml
GET /api/v1/leave/balance
Authorization: Bearer {ACCESS_TOKEN}
```

#### 4.4.7 연차 정책 조회
- Response DTO: `List<LeavePolicyResponseDto>`
```yaml
GET /api/v1/leave/policy
Authorization: Bearer {ACCESS_TOKEN}
```

### 4.5 특근 관리 API

#### 4.5.1 특근 신청
- Request DTO: `OvertimeRequestDto`
- Response DTO: `OvertimeResponseDto`
```yaml
POST /api/v1/overtime
Authorization: Bearer {ACCESS_TOKEN}

Request Body:
{
  "overtimeDate": "2026-04-12",
  "startTime": "19:00:00",
  "endTime": "21:00:00",
  "reason": "긴급 대응"
}
```

Validation Rules:
- 평일 특근 신청 불가
- 에러 코드: `INVALID_OVERTIME_DATE` (400)

#### 4.5.2 특근 단건 조회
- Response DTO: `OvertimeResponseDto`
```yaml
GET /api/v1/overtime/{id}
Authorization: Bearer {ACCESS_TOKEN}
```
- USER: 본인 건만
- TEAM_LEADER/ADMIN: 전체 조회 가능

#### 4.5.3 특근 목록 조회(권한 필터)
- Response DTO: `List<OvertimeResponseDto>`
```yaml
GET /api/v1/overtime/list
Authorization: Bearer {ACCESS_TOKEN}
```
- USER: 본인 내역만
- TEAM_LEADER/ADMIN: 전체 내역

#### 4.5.4 특근 승인
- Response DTO: `OvertimeResponseDto`
```yaml
PATCH /api/v1/overtime/{id}/approve
Authorization: Bearer {ACCESS_TOKEN}
```

#### 4.5.5 특근 반려
- Response DTO: `OvertimeResponseDto`
```yaml
PATCH /api/v1/overtime/{id}/reject
Authorization: Bearer {ACCESS_TOKEN}
```

#### 4.5.6 특근 내역 조회(호환)
- Response DTO: `List<OvertimeResponseDto>`
```yaml
GET /api/v1/overtime/my
Authorization: Bearer {ACCESS_TOKEN}
```

### 4.6 진행도 시각화 API
```yaml
GET /api/v1/dashboard/progress
Authorization: Bearer {ACCESS_TOKEN}
Required Role: USER, ADMIN

Response (200 OK):
{
  "success": true,
  "data": {
    "taskStatusStats": {
      "TODO": 5,
      "DOING": 7,
      "DONE": 8
    },
    "summary": {
      "totalTasks": 20,
      "completedTasks": 8,
      "completionRate": 40
    }
  }
}
```

---

## 5. 에러 코드 및 처리

### 5.1 표준 에러 코드 정의
| 코드 | HTTP 상태 | 설명 | 해결 방법 |
|------|-----------|------|-----------|
| **INVALID_INPUT_VALUE** | 400 | 입력값 검증 실패 | 요청 데이터 확인 후 재시도 |
| **LOGIN_ID_ALREADY_EXISTS** | 409 | 아이디 중복 | 다른 아이디 사용 |
| **EMAIL_ALREADY_EXISTS** | 409 | 이메일 중복 | 다른 이메일 사용 |
| **INVALID_CREDENTIALS** | 401 | 로그인 인증 실패 | 아이디/비밀번호 확인 |
| **UNAUTHORIZED** | 401 | 인증 필요 | 토큰 포함 후 재요청 |
| **ACCESS_DENIED** | 403 | 권한 없음 | 권한 확인 또는 관리자 문의 |
| **RESOURCE_NOT_FOUND** | 404 | 리소스 없음 | 요청 URL 및 ID 확인 |
| **DUPLICATE_RESOURCE** | 409 | 리소스 중복 생성 | 기존 리소스 확인 |
| **INVALID_TOKEN** | 401 | 유효하지 않은 토큰 | 재로그인 후 토큰 갱신 |
| **INTERNAL_SERVER_ERROR** | 500 | 서버 내부 오류 | 관리자 문의 |

### 5.2 비즈니스 로직 에러 코드
| 코드 | 설명 | 해결 방법 |
|------|------|-----------|
| **USER_NOT_FOUND** | 사용자를 찾을 수 없음 | 사용자 ID 확인 |
| **PROJECT_NOT_FOUND** | 프로젝트를 찾을 수 없음 | 프로젝트 ID 확인 |
| **TASK_NOT_FOUND** | Task를 찾을 수 없음 | Task ID 확인 |
| **TASK_ASSIGNMENT_NOT_FOUND** | Task 담당자 배정 정보를 찾을 수 없음 | taskId/userId 확인 |
| **PROJECT_MEMBER_NOT_FOUND** | 프로젝트 팀원 배정 정보를 찾을 수 없음 | projectId/userId 확인 |
| **TASK_ACCESS_DENIED** | 배정된 Task가 아님 | 담당자 권한 확인 |
| **DUPLICATE_PROJECT_MEMBER** | 동일 프로젝트에 중복 팀원 배정 시도 | 기존 팀원 배정 확인 |
| **DUPLICATE_TASK_ASSIGNMENT** | 동일 Task에 중복 담당자 배정 시도 | 기존 담당자 배정 확인 |
| **LEAVE_REQUEST_NOT_FOUND** | 연차 신청 건 없음 | 신청 ID 확인 |
| **LEAVE_DATE_NOT_WORKING_DAY** | 연차 신청 기간에 주말/공휴일 포함 | 신청 기간 재선택 |
| **LEAVE_INSUFFICIENT_BALANCE** | 잔여 연차 부족 | 잔여 연차 확인 |
| **OVERTIME_APPROVAL_REQUIRED** | 주말/공휴일 출근의 특근 승인 없음 | 특근 신청 승인 후 재시도 |
| **REJECT_REASON_REQUIRED** | 반려 사유 누락 | 반려 사유 입력 |

---

## 6. API 문서화

### 6.1 OpenAPI 3.0 스펙 예시
```yaml
openapi: 3.0.3
info:
  title: Mini ERP Groupware API
  description: 프로젝트 트래킹 및 연차 전자결재 API
  version: 1.0.0

servers:
  - url: https://localhost:8080/api/v1
    description: 로컬 개발 서버

components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

paths:
  /tasks:
    get:
      summary: 업무 목록 조회
      tags: [Tasks]
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            minimum: 0
            default: 0
      responses:
        '200':
          description: 성공
```

### 6.2 API 문서 생성 도구
```java
// Spring Boot + Swagger 설정
@Configuration
@EnableOpenApi
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini ERP Groupware API")
                        .version("v1.0.0")
                        .description("프로젝트 트래킹 및 연차 전자결재 REST API"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

## 7. 체크리스트 및 품질 관리

### 7.1 API 설계 체크리스트
```
□ RESTful 원칙을 준수하는가?
□ URL 네이밍 규칙이 일관되는가?
□ HTTP 메서드를 올바르게 사용하는가?
□ HTTP 상태 코드를 적절히 사용하는가?
□ 요청/응답 형식이 표준화되어 있는가?
□ 에러 응답이 명확하고 도움이 되는가?
□ 페이지네이션이 구현되어 있는가?
□ 적절한 인증/인가가 구현되어 있는가?
```

### 7.2 보안 체크리스트
```
□ 입력값 검증이 충분한가?
□ Access Token이 안전하게 관리되는가?
□ 권한 체크가 모든 엔드포인트에 적용되는가?
□ 에러 메시지에서 시스템 정보가 노출되지 않는가?
```

### 7.3 성능 체크리스트
```
□ N+1 쿼리 문제가 해결되었는가?
□ 데이터베이스 인덱스가 적절한가?
□ 페이지네이션 기본값/최대값이 정의되었는가?
```

## 8. 마무리

### 8.1 주요 포인트 요약
1. **RESTful 설계**: HTTP 메서드와 상태 코드의 올바른 사용
2. **일관성 유지**: 모든 API에서 동일한 규칙과 형식 적용
3. **보안 강화**: 인증, 인가, 입력 검증을 통한 보안 확보
4. **문서화**: 명확하고 완전한 API 문서 제공

### 8.2 추천 도구 및 라이브러리
- **문서화**: Swagger/OpenAPI, Postman

---

## 9. 0번 요구사항 반영 추가사항

### 9.1 페이지 목록 기준 API 매핑 고정
- 내 프로젝트/업무 페이지: `GET /projects`, `GET /tasks`, `GET /tasks/{taskId}`, `PATCH /tasks/{taskId}/status`
- 캘린더 페이지: `GET /calendar/events?year={year}&month={month}`, `GET /attendance/summary?month={yyyy-MM}`, `POST /attendance/check-in`
- 연차 신청 페이지: `POST /leave`, `GET /leave/balance`, `GET /leave/policy`
- 연차 신청 내역 페이지: `GET /leave/my`
- 프로젝트 생성 페이지(ADMIN): `POST /projects`
- 프로젝트 팀원 배정 페이지(ADMIN): `POST /projects/{projectId}/members`, `DELETE /projects/{projectId}/members/{userId}`
- 업무 생성 페이지(ADMIN): `POST /tasks`
- 업무 담당자 배정 페이지(ADMIN): `POST /tasks/{taskId}/assignments`, `DELETE /tasks/{taskId}/assignments/{userId}`
- 연차 승인 페이지(TEAM_LEADER/ADMIN): `GET /leave/all`, `PATCH /leave/{id}/approve`, `PATCH /leave/{id}/reject`
- 특근 승인 페이지(TEAM_LEADER/ADMIN): `GET /overtime/list`, `PATCH /overtime/{id}/approve`, `PATCH /overtime/{id}/reject`
- 업무 진행도 확인 페이지: `GET /dashboard/progress`, `GET /projects/{projectId}/progress`

### 9.2 권한 요구사항 기반 접근 제어 재명시
- USER: 본인에게 할당된 프로젝트/배정된 Task/본인 연차·특근 데이터만 조회 가능
- TEAM_LEADER: 일반 사용자 결재 처리, 특근/연차 목록 조회(권한 필터 적용)
- ADMIN(관리 소장): 전체 프로젝트/Task 관리, 사용자 권한 변경, 연차/특근 결재 처리

### 9.3 핵심 비즈니스 규칙 기반 API 처리 순서
- 연차 승인 API는 아래 순서로 처리
  1) 신청 상태 검증(`PENDING`)
  2) 계층 권한 검증(USER→TEAM_LEADER/ADMIN, TEAM_LEADER→ADMIN, ADMIN→본인)
  3) 승인 상태 변경
  4) 잔여 연차 차감
  5) 하나의 트랜잭션으로 커밋

### 9.4 확정 필요 항목 API 표기
- 출근 기록 API는 `POST /attendance/check-in`으로 운영
- 주말/공휴일 출근은 특근 승인 여부를 서버에서 검증하고, 미승인 시 `OVERTIME_APPROVAL_REQUIRED` 반환
- 특근 신청은 주말만 허용하며, 평일 신청 시 `INVALID_OVERTIME_DATE` 반환

### 9.5 MVP 포함 기능 API 체크
- [x] 회원가입
- [x] 로그인
- [x] 아이디 찾기
- [x] 비밀번호 찾기(3단계)
- [x] 프로젝트 생성/조회
- [x] 프로젝트 팀원 배정/해제
- [x] Task 생성/조회/상태 변경
- [x] Task 담당자 배정/해제
- [x] 연차 신청
- [x] 연차 승인/반려
- [x] 승인 시 연차 차감
- [x] 특근 신청/단건조회/목록조회
- [x] 특근 승인/반려
- [x] 업무 진행도 확인
- [x] 주말/공휴일 출근 시 특근 승인 검증

### 9.6 개발 전 필수 확정 8개
1. **Task 담당자 구조**: `TaskAssignment` 기반 다중 담당자만 사용(단일 assignee 혼용 금지)
2. **USER 조회 범위**: 할당된 업무/프로젝트 데이터만 조회 허용
3. **연차 데이터 위치**: `User` 기준(총/사용/잔여)
4. **usedDays 계산 주체**: 서버 계산(요청 본문 직접 입력 금지)
5. **인증 방식**: Access Token only
6. **승인-차감 처리 원칙**: 연차 승인 상태 변경과 연차 차감을 동일 트랜잭션으로 처리
7. **연차 신청 주말/공휴일 처리**: UI 차단 + 서버 검증(공휴일/대체공휴일 포함)으로 이중 방어
8. **주말/공휴일 출근 처리**: 특근 승인 사용자만 출근 기록 허용

---

**문서 끝**
