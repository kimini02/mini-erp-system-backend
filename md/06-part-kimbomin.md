# 김보민 - 인증/권한/공통 파트

## 담당 도메인
- `domain/user/` (회원가입, 로그인, 아이디찾기, 비밀번호찾기, 사용자 관리)
- `global/` (JWT, 인가 필터, 공통 예외/응답, Swagger 설정)

---

## 해야 할 일 요약

### 1순위: global 초기 세팅 (다른 파트 작업 전에 먼저 완성)
| 파일 | 할 일 |
|---|---|
| `SecurityConfig` | Spring Security 필터 체인, JWT 필터 등록, 공개 URL 설정 |
| `JwtTokenProvider` | Access Token 생성/검증/파싱 |
| `JwtAuthenticationFilter` | 요청 헤더에서 토큰 추출 -> 인증 객체 세팅 |
| `ApiResponse<T>` | 공통 성공 응답 (success, data, message, timestamp) |
| `ErrorResponse` | 공통 에러 응답 (statusCode, message, timestamp) |
| `ErrorCode` | 에러 코드 Enum 정의 |
| `BusinessException` | 커스텀 예외 클래스 |
| `GlobalExceptionHandler` | 전역 예외 핸들러 |
| `SwaggerConfig` | Swagger/OpenAPI 설정 |
| `BaseEntity` | id, createdAt, updatedAt (이미 완성) |
| `JpaAuditingConfig` | JPA Auditing 활성화 (이미 완성) |

### 2순위: 회원가입 / 로그인
| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/v1/auth/signup` | 회원가입 | 이메일 중복 체크, BCrypt 암호화 |
| `POST /api/v1/auth/login` | 로그인 | 이메일/비밀번호 검증 후 Access Token 발급 |

### 3순위: 아이디찾기 / 비밀번호찾기
| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/v1/auth/find-email` | 아이디(이메일) 찾기 | 이름 + 기타 정보로 이메일 조회 |
| `POST /api/v1/auth/find-password` | 비밀번호 찾기 | 이메일 인증 후 임시 비밀번호 발급 또는 재설정 |

### 4순위: 사용자 관리
| API | 메서드 | 설명 |
|---|---|---|
| `GET /api/v1/users` | 사용자 목록 조회 | ADMIN만, 페이지네이션 |
| `GET /api/v1/users/{userId}` | 사용자 상세 조회 | 본인 또는 ADMIN |
| `PUT /api/v1/users/{userId}` | 사용자 정보 수정 | 본인 또는 ADMIN |
| `PATCH /api/v1/users/{userId}/role` | 권한 변경 | ADMIN만 |

---

## 구현 시 주의사항
- JWT는 **Access Token only** (Refresh Token 없음)
- 비밀번호는 반드시 **BCrypt 암호화** 저장
- global 패키지는 다른 팀원이 의존하므로 **가장 먼저 완성**할 것
- User Entity에 연차 필드(totalAnnualLeave, usedAnnualLeave, remainingAnnualLeave)가 있음 -> 이세연 파트에서 사용

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
domain/user/dto/FindEmailRequestDto.java
domain/user/dto/FindPasswordRequestDto.java
```
