# SmartWork BE 공통 코딩 규칙

> **3명이 작성한 코드가 1명이 쓴 것처럼 보여야 한다.**
> 이 문서는 모든 BE 개발자가 코드 작성 전에 반드시 숙지해야 하는 규칙이다.
> 규칙을 변경할 때는 반드시 팀 합의 후 이 문서를 먼저 수정한다.

---

## 1. 기술 스택 (변경 금지)

| 항목 | 버전/도구 |
|------|-----------|
| Java | 17 |
| Spring Boot | 3.5.13 |
| DB | MariaDB 10.11 |
| ORM | Spring Data JPA |
| 빌드 | Maven |
| 인증 | JWT Access Token only |
| API 문서 | springdoc-openapi (Swagger) |
| 마이그레이션 | Flyway |

---

## 2. 패키지 구조 규칙

```
com.minierp.backend/
├── global/
│   ├── config/          ← 설정 클래스 (Security, Swagger, JpaAuditing 등)
│   ├── entity/          ← BaseEntity
│   ├── exception/       ← ErrorCode, BusinessException, GlobalExceptionHandler
│   ├── response/        ← ApiResponse, ErrorResponse
│   └── security/        ← JwtTokenProvider, JwtAuthenticationFilter
└── domain/
    └── {도메인명}/       ← user, project, task, approval, attendance, calendar, dashboard
        ├── controller/
        ├── service/
        ├── repository/
        ├── entity/
        └── dto/
```

### 절대 금지
- domain 밖에 dto, entity 패키지 만들지 않는다 (global/entity/BaseEntity만 예외)
- 도메인 간 controller → controller 호출 금지
- 도메인 간 참조가 필요하면 **Service → 다른 도메인의 Service** 또는 **Repository**를 통해서만 접근

---

## 3. Entity 규칙

### 3.1 클래스 선언부 (모든 Entity 동일)
```java
@Entity
@Table(name = "테이블명")          // 복수형 snake_case (예: users, tasks)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class EntityName extends BaseEntity {
    // ...
}
```

### 3.2 필수 준수 사항
| 규칙 | 설명 |
|------|------|
| `@Setter` 사용 금지 | 상태 변경은 반드시 **의미 있는 메서드**로 (예: `approve()`, `changeStatus()`) |
| `@Data` 사용 금지 | Entity에서 절대 사용하지 않는다 |
| `@Builder` Entity에 직접 사용 금지 | 정적 팩토리 메서드(`create()`, `of()`)를 사용한다 |
| BaseEntity 상속 필수 | `id`, `createdAt`, `updatedAt`는 BaseEntity에서 상속 |
| ID 전략 | `@GeneratedValue(strategy = GenerationType.IDENTITY)` 통일 |
| Enum 매핑 | `@Enumerated(EnumType.STRING)` 통일 (ORDINAL 금지) |
| 연관관계 기본 | `fetch = FetchType.LAZY` 통일 (EAGER 금지) |
| 컬렉션 초기화 | `new ArrayList<>()` 로 초기화 |

### 3.3 Entity 생성 패턴 (정적 팩토리 메서드)
```java
// 좋은 예
public static User create(String userName, String userEmail, String userPw, String positionName) {
    User user = new User();
    user.userName = userName;
    user.userEmail = userEmail;
    user.userPw = userPw;
    user.positionName = positionName;
    user.userRole = UserRole.USER;
    return user;
}

// 나쁜 예 - new + setter
User user = new User();
user.setUserName("김민희");  // @Setter 금지
```

### 3.4 상태 변경 메서드 네이밍
```java
// 좋은 예: 비즈니스 의미가 드러나는 이름
public void approve() { ... }
public void reject(String reason) { ... }
public void changeStatus(TaskStatus newStatus) { ... }
public void deductAnnualLeave(double days) { ... }

// 나쁜 예: setter 스타일
public void setStatus(TaskStatus status) { ... }
public void setAppStatus(LeaveStatus status) { ... }
```

### 3.5 컬럼 네이밍
| 구분 | Java 필드 | DB 컬럼 | 비고 |
|------|-----------|---------|------|
| 일반 | `camelCase` | `snake_case` | JPA 기본 전략 사용 |
| PK | `id` | `{테이블명}_id` 또는 `id` | `@Column(name = "user_id")` |
| FK | `{참조Entity}` | `{참조테이블}_id` | JPA가 자동 매핑 |
| Boolean | `isActive` | `is_active` | `is` 접두사 |
| Enum | `status` | `status` | `@Enumerated(EnumType.STRING)` |

---

## 4. DTO 규칙

### 4.1 네이밍 규칙
```
{도메인}{동작}{Request/Response}Dto

예:
  SignupRequestDto          ← 회원가입 요청
  LoginResponseDto          ← 로그인 응답
  TaskCreateRequestDto      ← Task 생성 요청
  TaskResponseDto           ← Task 조회 응답
  LeaveRequestCreateDto     ← 연차신청 요청
  LeaveRequestResponseDto   ← 연차신청 응답
```

### 4.2 DTO 패턴
```java
// Request DTO - 클래스 기반
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskCreateRequestDto {

    @NotBlank(message = "업무 제목은 필수입니다")
    private String taskTitle;

    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;

    // 테스트용 생성자 또는 정적 팩토리
    public static TaskCreateRequestDto of(String taskTitle, LocalDate startDate) {
        TaskCreateRequestDto dto = new TaskCreateRequestDto();
        dto.taskTitle = taskTitle;
        dto.startDate = startDate;
        return dto;
    }
}
```

```java
// Response DTO - Entity → DTO 변환은 from() 정적 메서드
@Getter
@AllArgsConstructor
public class TaskResponseDto {

    private Long id;
    private String taskTitle;
    private String taskStatus;

    public static TaskResponseDto from(Task task) {
        return new TaskResponseDto(
            task.getId(),
            task.getTaskTitle(),
            task.getTaskStatus().name()
        );
    }
}
```

### 4.3 필수 준수 사항
| 규칙 | 설명 |
|------|------|
| Request/Response 분리 | 하나의 DTO로 요청/응답 겸용 금지 |
| Entity 직접 반환 금지 | Controller에서 Entity를 그대로 JSON 응답하지 않는다 |
| `from()` 메서드 필수 | Response DTO는 반드시 `from(Entity)` 정적 메서드로 변환 |
| Validation 어노테이션 | Request DTO 필드에 `@NotBlank`, `@NotNull`, `@Size` 등 명시 |
| DTO 위치 | 반드시 해당 도메인의 `dto/` 패키지 안에 위치 |

---

## 5. Controller 규칙

### 5.1 기본 구조
```java
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponseDto>> createTask(
            @Valid @RequestBody TaskCreateRequestDto request) {
        TaskResponseDto response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "업무가 생성되었습니다"));
    }
}
```

### 5.2 필수 준수 사항
| 규칙 | 설명 |
|------|------|
| 비즈니스 로직 금지 | Controller에 if문, 계산, DB 호출 등 금지. Service에 위임만 한다 |
| `@Valid` 필수 | Request DTO 앞에 반드시 `@Valid` 붙인다 |
| 반환 타입 | `ResponseEntity<ApiResponse<T>>` 통일 |
| URL 규칙 | 소문자, 하이픈, 복수형 명사 (예: `/api/v1/leave-requests`) |
| HTTP 메서드 | 설계서(03-rest-api.md) 기준 엄수 |
| 상태 코드 | 생성 201, 조회 200, 삭제/해제 204, 에러는 ErrorCode에서 관리 |

---

## 6. Service 규칙

### 6.1 기본 구조
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;  // 다른 도메인 Repository 주입 허용

    @Transactional
    public TaskResponseDto createTask(TaskCreateRequestDto request) {
        // 1. 검증
        // 2. Entity 생성
        // 3. 저장
        // 4. DTO 변환 후 반환
    }

    public TaskResponseDto getTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        return TaskResponseDto.from(task);
    }
}
```

### 6.2 필수 준수 사항
| 규칙 | 설명 |
|------|------|
| 클래스 레벨 `@Transactional(readOnly = true)` | 기본은 읽기 전용, 쓰기 메서드만 `@Transactional` 추가 |
| 조회 실패 시 | `orElseThrow(() -> new BusinessException(ErrorCode.XXX_NOT_FOUND))` 통일 |
| 직접 SQL/JDBC 금지 | Repository 메서드만 사용 (필요 시 `@Query` JPQL) |
| Service에 `@Autowired` 금지 | `@RequiredArgsConstructor` + `private final` 통일 |

---

## 7. Repository 규칙

### 7.1 기본 구조
```java
public interface TaskRepository extends JpaRepository<Task, Long> {

    // 메서드 네이밍 쿼리
    List<Task> findByProjectId(Long projectId);

    // 복잡한 쿼리만 @Query 사용
    @Query("SELECT t FROM Task t JOIN t.taskAssignments ta WHERE ta.user.id = :userId")
    List<Task> findByAssigneeUserId(@Param("userId") Long userId);
}
```

### 7.2 필수 준수 사항
| 규칙 | 설명 |
|------|------|
| `JpaRepository` 상속 | `CrudRepository` 사용하지 않는다 |
| 네이밍 쿼리 우선 | 단순 조건은 메서드명으로 해결, 복잡할 때만 `@Query` |
| Native Query 금지 | 가급적 JPQL 사용. Native Query 필요 시 팀 합의 후 사용 |
| 페이지네이션 | `Pageable` 파라미터 + `Page<Entity>` 반환 |

---

## 8. 예외 처리 규칙

### 8.1 ErrorCode 정의 규칙
```java
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 유효하지 않습니다"),

    // User 도메인 (김보민)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),

    // Task 도메인 (김민희)
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "업무를 찾을 수 없습니다"),
    DUPLICATE_ASSIGNMENT(HttpStatus.CONFLICT, "이미 배정된 담당자입니다"),

    // Approval 도메인 (이세연)
    LEAVE_NOT_FOUND(HttpStatus.NOT_FOUND, "연차 신청을 찾을 수 없습니다"),
    INSUFFICIENT_LEAVE(HttpStatus.UNPROCESSABLE_ENTITY, "잔여 연차가 부족합니다");

    private final HttpStatus status;
    private final String message;
}
```

### 8.2 예외 사용 패턴
```java
// 통일된 예외 던지기
throw new BusinessException(ErrorCode.USER_NOT_FOUND);

// GlobalExceptionHandler가 일괄 처리 → ErrorResponse 반환
```

### 8.3 금지 사항
- `try-catch`로 예외를 삼키지 않는다 (로깅 후 재던지기는 가능)
- Controller에서 직접 예외 처리하지 않는다 → `GlobalExceptionHandler`에 위임
- `RuntimeException`을 직접 던지지 않는다 → `BusinessException` 사용
- 각 도메인별 커스텀 Exception 클래스 만들지 않는다 → `ErrorCode`로 구분

---

## 9. 공통 응답 형식

### 9.1 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "message": "요청이 성공적으로 처리되었습니다",
  "timestamp": "2026-03-28T10:30:00Z"
}
```

### 9.2 에러 응답
```json
{
  "statusCode": "400",
  "message": "입력값이 유효하지 않습니다",
  "timestamp": "2026-03-28T10:30:00Z"
}
```

### 9.3 규칙
- 모든 API 응답은 `ApiResponse<T>` 또는 `ErrorResponse`를 사용한다
- 직접 `Map`, `String`, `Entity`를 반환하지 않는다
- 페이지네이션 응답은 `ApiResponse<Page<ResponseDto>>` 형태

---

## 10. 네이밍 컨벤션 종합

### 10.1 클래스 네이밍
| 레이어 | 규칙 | 예시 |
|--------|------|------|
| Entity | 도메인 명사 PascalCase | `User`, `Task`, `LeaveRequest` |
| DTO | `{도메인}{동작}{Request/Response}Dto` | `TaskCreateRequestDto` |
| Controller | `{도메인}Controller` | `TaskController`, `AuthController` |
| Service | `{도메인}Service` | `TaskService`, `ApprovalService` |
| Repository | `{Entity}Repository` | `TaskRepository`, `LeaveRequestRepository` |
| Enum | 도메인 의미 PascalCase | `TaskStatus`, `LeaveType`, `UserRole` |

### 10.2 메서드 네이밍
| 동작 | Controller | Service |
|------|-----------|---------|
| 생성 | `create{Entity}` | `create{Entity}` |
| 단건 조회 | `get{Entity}` | `get{Entity}` |
| 목록 조회 | `get{Entity}s` 또는 `get{Entity}List` | `get{Entity}s` |
| 수정 | `update{Entity}` | `update{Entity}` |
| 삭제 | `delete{Entity}` | `delete{Entity}` |
| 상태변경 | `update{Entity}Status` | `change{Action}` (예: `approve`, `reject`) |

### 10.3 변수 네이밍
| 구분 | 규칙 | 좋은 예 | 나쁜 예 |
|------|------|---------|---------|
| 지역변수 | camelCase, 의미 명확 | `savedTask`, `leaveRequest` | `t`, `lr`, `data` |
| 상수 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` | `maxPageSize` |
| Boolean | `is/has/can` 접두사 | `isOverdue`, `hasPermission` | `overdue`, `permission` |

---

## 11. Enum 규칙

### 11.1 Enum 위치
- 해당 도메인의 `entity/` 패키지에 위치 (DTO 아님)
- 예: `domain/task/entity/TaskStatus.java`

### 11.2 Enum 작성 패턴
```java
@Getter
@RequiredArgsConstructor
public enum TaskStatus {

    TODO("할 일"),
    DOING("진행 중"),
    DONE("완료");

    private final String description;
}
```

---

## 12. Git 브랜치 & 커밋 규칙

### 12.1 브랜치 전략
```
main              ← 배포 브랜치 (직접 push 금지)
└── develop       ← 개발 통합 브랜치
    ├── feat/user-signup         ← 김보민
    ├── feat/task-crud           ← 김민희
    └── feat/leave-request       ← 이세연
```

### 12.2 브랜치 네이밍
```
feat/{도메인}-{기능}     ← 기능 개발
fix/{도메인}-{내용}      ← 버그 수정
refactor/{도메인}-{내용} ← 리팩토링
```

### 12.3 커밋 메시지 형식
```
{타입}: {간결한 설명}

타입:
  feat     - 새 기능
  fix      - 버그 수정
  refactor - 리팩토링 (기능 변경 없음)
  docs     - 문서 수정
  test     - 테스트 추가/수정
  chore    - 설정, 빌드 관련

예시:
  feat: User 회원가입 API 구현
  fix: 연차 차감 트랜잭션 롤백 누락 수정
  refactor: TaskService 조회 로직 분리
```

### 12.4 PR 규칙
- develop 브랜치로 PR 생성
- PR 제목: 커밋 메시지와 동일한 형식
- 최소 1명 이상 코드 리뷰 후 머지
- 충돌 발생 시: 본인 브랜치에서 develop을 merge 받아 해결 후 push

---

## 13. 충돌 방지 규칙 (3명 분업 핵심)

### 13.1 파일 소유권
| 파일/패키지 | 담당자 | 다른 사람이 수정할 때 |
|------------|--------|---------------------|
| `global/` 전체 | 김보민 | 반드시 김보민에게 요청 |
| `domain/user/` | 김보민 | User Entity 필드 추가 시 김보민과 합의 |
| `domain/project/`, `domain/task/`, `domain/dashboard/` | 김민희 | 김민희에게 요청 |
| `domain/approval/`, `domain/attendance/`, `domain/calendar/` | 이세연 | 이세연에게 요청 |
| `ErrorCode` enum | 공동 | 각자 담당 도메인 에러코드만 추가. 주석으로 영역 구분 |
| `pom.xml` | 공동 | 의존성 추가 시 팀 슬랙에 공유 후 추가 |

### 13.2 공유 파일 수정 시 규칙
- `ErrorCode.java` — 각자 담당 도메인 블록에만 추가 (주석 구분)
- `SecurityConfig.java` — URL 허용 목록 추가 시 김보민에게 요청
- `application.yml` — 설정 추가 시 팀 채널에 사전 공유

### 13.3 타 도메인 Entity 사용 시
```java
// 허용: 다른 도메인의 Repository를 Service에서 주입
@Service
public class ApprovalService {
    private final UserRepository userRepository;  // OK
}

// 금지: 다른 도메인의 Entity 내부 로직을 직접 수정
// 이세연이 User.deductAnnualLeave() 메서드가 필요하면
// → 김보민에게 User Entity에 메서드 추가 요청
// 또는 → 팀 합의 후 직접 추가하되 PR에서 알림
```

---

## 14. 테스트 규칙

### 14.1 테스트 파일 위치
```
src/test/java/com/minierp/backend/
└── domain/
    └── {도메인명}/
        ├── service/       ← Service 통합 테스트
        ├── entity/        ← Entity 단위 테스트
        └── controller/    ← Controller 슬라이스 테스트 (선택)
```

### 14.2 테스트 네이밍
```java
@DisplayName("연차 승인 시 잔여 연차가 차감된다")
@Test
void approveLeave_deductsRemainingLeave() {
    // given
    // when
    // then
}
```

### 14.3 테스트 작성 기준
| 대상 | 테스트 방식 | 어노테이션 |
|------|-----------|-----------|
| Entity | 순수 단위 테스트 (Spring 없이) | 없음 |
| Service | `@SpringBootTest` 통합 테스트 | `@SpringBootTest` + `@Transactional` |
| Controller | 필요 시 `@WebMvcTest` | 선택사항 |

---

## 15. 절대 하지 말 것 체크리스트

| # | 금지 사항 | 이유 |
|---|----------|------|
| 1 | Controller에 비즈니스 로직 | 레이어 책임 분리 위반 |
| 2 | Entity를 API 응답으로 직접 반환 | 순환 참조, 불필요한 데이터 노출 |
| 3 | `@Setter` Entity에 사용 | 무분별한 상태 변경 방지 |
| 4 | `@Data` Entity에 사용 | equals/hashCode/toString 예측 불가 |
| 5 | `FetchType.EAGER` 사용 | N+1 문제, 성능 저하 |
| 6 | `@Enumerated(EnumType.ORDINAL)` | 순서 변경 시 데이터 깨짐 |
| 7 | Native Query 무단 사용 | DB 종속성, 팀 합의 필요 |
| 8 | 다른 사람 담당 패키지 무단 수정 | Git 충돌, 책임 불명확 |
| 9 | `try-catch`로 예외 삼키기 | 장애 원인 추적 불가 |
| 10 | 커밋 메시지에 "수정", "변경" 단독 사용 | 무엇을 왜 바꿨는지 알 수 없음 |
| 11 | `@Autowired` 필드 주입 | 테스트 어려움, 생성자 주입 사용 |
| 12 | `System.out.println` 사용 | Slf4j 로거 사용 |

---

## 부록: 코드 리뷰 체크리스트 (PR 리뷰 시 확인)

- [ ] Entity에 `@Setter`, `@Data` 없는가?
- [ ] DTO에 `from()` 정적 메서드가 있는가?
- [ ] Controller에 비즈니스 로직이 없는가?
- [ ] `@Valid`가 Request DTO 앞에 있는가?
- [ ] 반환 타입이 `ApiResponse<T>`인가?
- [ ] 예외 처리가 `BusinessException` + `ErrorCode`인가?
- [ ] 조회 메서드에 `@Transactional(readOnly = true)`가 있는가?
- [ ] 연관관계가 `LAZY`인가?
- [ ] 커밋 메시지 형식을 지켰는가?
- [ ] 다른 사람 담당 패키지를 수정하지 않았는가?

---

**문서 버전**: v1.0
**최초 작성**: 2026-03-31
**다음 리뷰**: 구현 착수 1주 후
