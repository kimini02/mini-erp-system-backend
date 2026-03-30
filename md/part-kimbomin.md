# 김보민 - 인증/사용자/근태 파트

## 담당 도메인
- `domain/user/` (인증 + 사용자 관리)
- `domain/attendance/` (근태/출퇴근)
- `global/` (공통 설정 - 초기 세팅)

---

## 해야 할 일 요약

### 1순위: global 초기 세팅 (다른 파트 작업 전에 먼저 완성)
| 파일 | 할 일 |
|---|---|
| `BaseEntity` | id, createdAt, updatedAt 필드 구현 (이미 완성) |
| `SecurityConfig` | Spring Security 필터 체인, JWT 필터 등록, 공개 URL 설정 |
| `JwtTokenProvider` | Access Token 생성/검증/파싱 구현 |
| `JwtAuthenticationFilter` | 요청 헤더에서 토큰 추출 -> 인증 객체 세팅 |
| `ApiResponse<T>` | 공통 성공 응답 (success, data, message, timestamp) |
| `ErrorResponse` | 공통 에러 응답 (statusCode, message, timestamp) |
| `ErrorCode` | 에러 코드 Enum 정의 |
| `BusinessException` | 커스텀 예외 클래스 |
| `GlobalExceptionHandler` | 전역 예외 핸들러 |
| `SwaggerConfig` | Swagger/OpenAPI 설정 |

### 2순위: 회원가입 / 로그인
| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/v1/auth/signup` | 회원가입 | 이메일 중복 체크, 비밀번호 BCrypt 암호화 |
| `POST /api/v1/auth/login` | 로그인 | 이메일/비밀번호 검증 후 Access Token 발급 |

### 3순위: 사용자 관리
| API | 메서드 | 설명 |
|---|---|---|
| `GET /api/v1/users` | 사용자 목록 조회 | ADMIN만 가능, 페이지네이션 |
| `GET /api/v1/users/{userId}` | 사용자 상세 조회 | 본인 또는 ADMIN |
| `PUT /api/v1/users/{userId}` | 사용자 정보 수정 | 본인 또는 ADMIN |
| `PATCH /api/v1/users/{userId}/role` | 권한 변경 | ADMIN만 가능 |

### 4순위: 근태
| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/v1/attendance/check-in` | 출근 체크인 | 주말/공휴일 특근 승인 검증 필수 |
| `GET /api/v1/attendance/summary` | 근태 요약 조회 | 월별 출근일수, 지각 등 |

---

## 구현 시 주의사항
- JWT는 **Access Token only** (Refresh Token 없음)
- 비밀번호는 반드시 **BCrypt 암호화** 저장
- global 패키지는 다른 팀원이 의존하므로 **가장 먼저 완성**할 것
- User Entity에 연차 필드(totalAnnualLeave, usedAnnualLeave, remainingAnnualLeave)가 있음 -> 이세연 파트와 연동 필요
- 주말/공휴일 출근은 특근 승인 사용자만 허용 (미승인 시 예외 반환)

---

## 작업 파일 목록
```
global/config/SecurityConfig.java
global/config/JpaAuditingConfig.java
global/config/SwaggerConfig.java
global/entity/BaseEntity.java
global/exception/GlobalExceptionHandler.java
global/exception/ErrorCode.java
global/exception/BusinessException.java
global/response/ApiResponse.java
global/response/ErrorResponse.java
global/security/JwtTokenProvider.java
global/security/JwtAuthenticationFilter.java
domain/user/entity/User.java
domain/user/entity/UserRole.java
domain/user/controller/AuthController.java
domain/user/controller/UserController.java
domain/user/service/AuthService.java
domain/user/service/UserService.java
domain/user/repository/UserRepository.java
domain/user/dto/SignupRequestDto.java
domain/user/dto/LoginRequestDto.java
domain/user/dto/LoginResponseDto.java
domain/user/dto/UserResponseDto.java
domain/user/dto/UserUpdateRequestDto.java
domain/attendance/entity/Attendance.java
domain/attendance/entity/AttendanceStatus.java
domain/attendance/controller/AttendanceController.java
domain/attendance/service/AttendanceService.java
domain/attendance/repository/AttendanceRepository.java
domain/attendance/dto/CheckInRequestDto.java
domain/attendance/dto/AttendanceSummaryDto.java
```
