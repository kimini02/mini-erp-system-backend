# Entity 설계서

## 문서 정보
- **프로젝트명**: 사내 프로젝트 관리 및 결재 통합 시스템사내 프로젝트/업무 트래킹과 연차/휴가 전자결재를 하나로 합친 그룹웨어
- **작성자**: [팀명/김보민]
- **작성일**: [2026-04-03]
- **버전**: [v1.0]
- **검토자**: [김보민, 김민희, 이새연]
- **승인자**: [이채현]

---

## 1. Entity 설계 개요

### 1.1 설계 목적
> JPA Entity 클래스 설계를 통해 객체-관계 매핑(ORM)을 정의하고,
> 비즈니스 도메인을 코드로 표현하여 유지보수가 용이한 시스템을 구축

### 1.2 설계 원칙
- **단일 책임 원칙**: 하나의 Entity는 하나의 비즈니스 개념만 표현
- **캡슐화**: 비즈니스 로직을 Entity 내부에 구현
- **불변성**: 가능한 한 불변 객체로 설계
- **연관관계 최소화**: 필요한 관계만 매핑하여 복잡도 감소

### 1.3 기술 스택
- **ORM 프레임워크**: Spring Data JPA
- **데이터베이스**: MariaDB 10.11
- **검증 프레임워크**: Bean Validation
- **감사 기능**: Spring Data JPA Auditing

---

## 2. Entity 목록 및 분류

### 2.1 Entity 분류 매트릭스
| Entity명 | 유형 | 비즈니스 중요도 | 기술적 복잡도 | 우선순위 |
|----------|------|----------------|---------------|----------|
| **User** | 핵심 | 높음 | 중간 | 1순위 |
| **Project** | 핵심 | 높음 | 중간 | 1순위 |
| **Task** | 핵심 | 높음 | 중간 | 1순위 |
| **LeaveRequest** | 핵심 | 높음 | 높음 | 1순위 |
| **OvertimeRequest** | 핵심 | 높음 | 중간 | 1순위 |
| **Attendance** | 지원 | 중간 | 중간 | 2순위 |
| **ProjectMember** | 지원 | 중간 | 낮음 | 2순위 |
| **TaskAssignment** | 지원 | 중간 | 낮음 | 2순위 |

### 2.2 Entity 상속 구조

모든 Entity는 `BaseEntity`(`id`, `createdAt`, `updatedAt`)를 상속합니다.

---

## 3. 공통 설계 규칙

### 3.1 네이밍 규칙
| 구분 | 규칙 | 예시 | 비고 |
|------|------|------|------|
| **Entity 클래스명** | PascalCase | `User`, `LeaveRequest` | 단수형 사용 |
| **테이블명** | snake_case | `users`, `leave_requests` | 복수형 사용 |
| **컬럼명** | snake_case | `user_id`, `created_at` | 언더스코어 구분 |
| **연관관계 필드** | camelCase | `projectMembers`, `taskAssignments` | 객체 참조명 |
| **boolean 필드** | is + 형용사 | `isActive` | 명확한 의미 |

### 3.2 공통 어노테이션 규칙
```java
@Entity
@Table(name = "table_name")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class EntityName extends BaseEntity {
    // 필드 정의
}
```

### 3.3 ID 생성 전략
| Entity | 전략 | 이유 | 예시 |
|--------|------|------|------|
| **User** | IDENTITY | MariaDB Auto Increment 활용 | 1, 2, 3, ... |
| **Project** | IDENTITY | 프로젝트 생성 순서 보장 | 1, 2, 3, ... |
| **Task** | IDENTITY | 업무 생성 순서 보장 | 1, 2, 3, ... |
| **LeaveRequest** | IDENTITY | 결재 이력 순서 보장 | 1, 2, 3, ... |
| **OvertimeRequest** | IDENTITY | 신청 순서 보장 | 1, 2, 3, ... |
| **Attendance** | IDENTITY | 근태 레코드 관리 단순화 | 1, 2, 3, ... |

---

## 4. 상세 Entity 설계

### 4.1 User Entity

#### 4.1.1 기본 정보
```java
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {
    // 필드 정의 (아래 상세 명세 참조)
}
```

#### 4.1.2 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|---------------|
| **id** | `Long` | `id` | PK, NOT NULL, AUTO_INCREMENT | 사용자 식별자 | 시스템 자동 생성 |
| **loginId** | `String` | `login_id` | UNIQUE, NOT NULL, LENGTH(50) | 로그인 아이디 | 중복 불가 |
| **userName** | `String` | `user_name` | NOT NULL, LENGTH(50) | 이름 | 필수 입력 |
| **userEmail** | `String` | `user_email` | UNIQUE, NOT NULL, LENGTH(100) | 이메일 | 중복 불가 |
| **userPw** | `String` | `user_pw` | NOT NULL, LENGTH(255) | 암호화 비밀번호 | BCrypt 저장 |
| **departmentCode** | `String` | `department_code` | NOT NULL, LENGTH(2) | 부서 코드 | 01/02/03 저장 |
| **positionName** | `String` | `position_name` | NOT NULL, LENGTH(30) | 직책 | 사원/대리/과장/팀장/관리소장 |
| **assignRole** | `String` | `assign_role` | NULL 허용 | 보조 역할 | 특근 승인 플래그 등에 사용 |
| **userRole** | `UserRole` | `user_role` | NOT NULL, ENUM | 시스템 권한 | USER/TEAM_LEADER/ADMIN |
| **isActive** | `boolean` | `is_active` | NOT NULL | 활성 여부 | 기본 true |
| **totalAnnualLeave** | `BigDecimal` | `total_annual_leave` | NOT NULL | 총 연차 | 직책별 초기값 부여 |
| **usedAnnualLeave** | `BigDecimal` | `used_annual_leave` | NOT NULL | 사용 연차 | 승인 시 증가 |
| **remainingAnnualLeave** | `BigDecimal` | `remaining_annual_leave` | NOT NULL | 잔여 연차 | 승인 시 차감 |

직책별 초기 연차 정책:
- 사원 14.0
- 대리 16.0
- 과장 17.0
- 팀장 18.0
- 관리소장/관리자 19.0

부서 코드 정책:
- 개발팀 `01`
- 유지보수팀 `02`
- 모바일개발팀 `03`

#### 4.1.3 핵심 비즈니스 메서드
```java
public void changeRole(UserRole userRole)
public void updateProfile(String userName, String positionName)
public void updatePassword(String newEncodedPassword)
public void deductAnnualLeave(BigDecimal usedDays)
```

### 4.2 Project Entity

#### 4.2.1 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|---------------|
| **id** | `Long` | `id` | PK, NOT NULL, AUTO_INCREMENT | 프로젝트 식별자 | 시스템 자동 생성 |
| **title** | `String` | `title` | NOT NULL, LENGTH(100) | 프로젝트명 | 필수 |
| **content** | `String` | `content` | LENGTH(1000) | 설명 | 선택 |
| **startDate** | `LocalDate` | `start_date` | NOT NULL | 시작일 | 종료일 이전 |
| **endDate** | `LocalDate` | `end_date` | NOT NULL | 종료일 | 시작일 이후 |
| **status** | `ProjectStatus` | `status` | NOT NULL, ENUM | 상태 | READY/PROGRESS/DONE/HOLD |
| **priority** | `Priority` | `priority` | NOT NULL, ENUM | 우선순위 | HIGH/MEDIUM/LOW |
| **leader** | `User` | `leader_id` | FK, NULL 허용 | 담당 팀장 | TEAM_LEADER 권장 |

#### 4.2.2 연관관계
- `projectMembers` (1:N)
- `tasks` (1:N)

#### 4.2.3 핵심 메서드
```java
public static Project create(...)
public void update(...)
public void assignLeader(User leader)
public void updateStatusByTasks()
```

### 4.3 Task Entity

#### 4.3.1 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|---------------|
| **id** | `Long` | `id` | PK, NOT NULL, AUTO_INCREMENT | 업무 식별자 | 시스템 자동 생성 |
| **taskTitle** | `String` | `task_title` | NOT NULL, LENGTH(100) | 제목 | 필수 |
| **taskContent** | `String` | `task_content` | NOT NULL, LENGTH(1000) | 내용 | 필수 |
| **endDate** | `LocalDate` | `end_date` | NOT NULL | 마감일 | 프로젝트 종료일 초과 불가 |
| **taskStatus** | `TaskStatus` | `task_status` | NOT NULL, ENUM | 상태 | TODO/DOING/DONE |
| **priority** | `Priority` | `priority` | NOT NULL, ENUM | 우선순위 | HIGH/MEDIUM/LOW |
| **project** | `Project` | `project_id` | FK, NOT NULL | 프로젝트 참조 | 필수 |

#### 4.3.2 핵심 메서드
```java
public static Task create(...)
public void update(...)
public void changeStatus(TaskStatus newStatus)
public boolean isOverdue()
```

### 4.4 LeaveRequest Entity

#### 4.4.1 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|---------------|
| **id** | `Long` | `id` | PK, NOT NULL, AUTO_INCREMENT | 결재 식별자 | 시스템 자동 생성 |
| **requester** | `User` | `requester_id` | FK, NOT NULL | 신청자 | 필수 |
| **approver** | `User` | `approver_id` | FK, NULL 허용 | 승인자 | 처리 시 입력 |
| **appType** | `LeaveType` | `app_type` | NOT NULL, ENUM | 신청 유형 | ANNUAL/HALF_* |
| **startDate** | `LocalDate` | `start_date` | NOT NULL | 시작일 | 종료일 이전 |
| **endDate** | `LocalDate` | `end_date` | NOT NULL | 종료일 | 시작일 이후 |
| **usedDays** | `BigDecimal` | `used_days` | NOT NULL | 사용 일수 | 서버 계산 |
| **appStatus** | `LeaveStatus` | `app_status` | NOT NULL, ENUM | 상태 | PENDING/APPROVED/REJECTED/CANCELLED |
| **requestReason** | `String` | `request_reason` | LENGTH(500) | 신청 사유 | 선택 입력 |
| **rejectReason** | `String` | `reject_reason` | LENGTH(500) | 반려 사유 | 반려 시 필수 |

#### 4.4.2 핵심 메서드
```java
public void approve(User approver)
public void reject(User approver, String reason)
public void cancel(User requester)
public void calculateUsedDays(List<LocalDate> holidayList)
```

### 4.5 OvertimeRequest Entity

#### 4.5.1 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|---------------|
| **id** | `Long` | `id` | PK, NOT NULL, AUTO_INCREMENT | 특근 식별자 | 시스템 자동 생성 |
| **requester** | `User` | `requester_id` | FK, NOT NULL | 신청자 | 필수 |
| **approver** | `User` | `approver_id` | FK, NULL 허용 | 처리자 | 승인/반려 시 설정 |
| **overtimeDate** | `LocalDate` | `overtime_date` | NOT NULL | 특근 일자 | 주말 신청만 허용 |
| **startTime** | `LocalTime` | `start_time` | NOT NULL | 시작 시간 | 종료시간 이전 |
| **endTime** | `LocalTime` | `end_time` | NOT NULL | 종료 시간 | 시작시간 이후 |
| **reason** | `String` | `reason` | LENGTH(500) | 사유 | 선택 |
| **status** | `OvertimeStatus` | `status` | NOT NULL, ENUM | 상태 | PENDING/APPROVED/REJECTED |

#### 4.5.2 핵심 메서드
```java
public void approve(User approver)
public void reject(User approver)
```

### 4.6 Attendance Entity

#### 4.6.1 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|---------------|
| **id** | `Long` | `id` | PK, NOT NULL, AUTO_INCREMENT | 근태 식별자 | 시스템 자동 생성 |
| **user** | `User` | `user_id` | FK, NOT NULL | 사용자 | 필수 |
| **workDate** | `LocalDate` | `work_date` | NOT NULL | 근무일 | 사용자+일자 유니크 |
| **clockInTime** | `LocalTime` | `clock_in_time` | NULL 허용 | 출근시간 | 체크인 시 입력 |
| **clockOutTime** | `LocalTime` | `clock_out_time` | NULL 허용 | 퇴근시간 | 체크아웃 시 입력 |
| **attStatus** | `AttendanceStatus` | `att_status` | NOT NULL, ENUM | 근태 상태 | NORMAL/LATE/ABSENT/LEAVE |

#### 4.6.2 핵심 메서드
```java
public static Attendance checkIn(User user, LocalDate workDate, LocalTime clockInTime)
public void checkOut(LocalTime clockOutTime)
public void updateAttendance(LocalTime clockInTime, LocalTime clockOutTime)
```

### 4.7 ProjectMember Entity

#### 4.7.1 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 |
|--------|-------------|--------|----------|------|
| **id** | `Long` | `id` | PK, NOT NULL, AUTO_INCREMENT | 식별자 |
| **project** | `Project` | `project_id` | FK, NOT NULL | 프로젝트 |
| **user** | `User` | `user_id` | FK, NOT NULL | 배정 사용자 |

유니크 제약:
- `(project_id, user_id)`

### 4.8 TaskAssignment Entity

#### 4.8.1 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 |
|--------|-------------|--------|----------|------|
| **id** | `Long` | `id` | PK, NOT NULL, AUTO_INCREMENT | 식별자 |
| **task** | `Task` | `task_id` | FK, NOT NULL | 업무 |
| **user** | `User` | `user_id` | FK, NOT NULL | 담당자 |

유니크 제약:
- `(task_id, user_id)`

---

## 5. Enum 타입 정의

### 5.1 UserRole
```java
public enum UserRole {
    USER,
    TEAM_LEADER,
    ADMIN
}
```

### 5.2 ProjectStatus
```java
public enum ProjectStatus {
    READY,
    PROGRESS,
    DONE,
    HOLD
}
```

### 5.3 TaskStatus
```java
public enum TaskStatus {
    TODO,
    DOING,
    DONE
}
```

### 5.4 LeaveStatus
```java
public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
```

### 5.5 OvertimeStatus
```java
public enum OvertimeStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
```

---

## 6. 연관관계 매핑 전략

### 6.1 연관관계 매핑 규칙
| 관계 유형 | 기본 전략 | 이유 | 예외 상황 |
|----------|-----------|------|-----------|
| **@ManyToOne** | LAZY | 성능 최적화 | 명시적 Fetch Join 사용 |
| **@OneToMany** | LAZY | N+1 문제 방지 | 필요한 경우 EntityGraph |
| **@OneToOne** | 지양 | 도메인 단순화 | 필요 시 도입 |
| **@ManyToMany** | 사용 금지 | 제약/확장 어려움 | 중간 매핑 엔티티 사용 |

### 6.2 Cascade 옵션 가이드
```java
@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
private List<TaskAssignment> taskAssignments = new ArrayList<>();

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "project_id", nullable = false)
private Project project;
```

### 6.3 양방향 연관관계 관리
```java
void addTaskAssignment(TaskAssignment taskAssignment) {
    taskAssignments.add(taskAssignment);
}
```

---

## 7. 감사(Auditing) 설정

### 7.1 BaseEntity 구현
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 7.2 Auditing 설정
```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

---

## 8. 성능 최적화 전략

### 8.1 N+1 문제 해결
```java
@Query("SELECT lr FROM LeaveRequest lr " +
       "WHERE lr.appStatus = :appStatus " +
       "AND lr.startDate <= :endDate " +
       "AND lr.endDate >= :startDate")
List<LeaveRequest> findOverlappingLeaves(...)
```

### 8.2 쿼리 최적화
```java
Page<User> findAll(Specification<User> spec, Pageable pageable)
```

---

## 9. 검증 및 제약조건

### 9.1 Bean Validation 어노테이션
```java
@NotBlank(message = "아이디는 필수입니다.")
private String id;
```

### 9.2 데이터베이스 제약조건
```java
@Table(name = "attendances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "work_date"})
})
```

```java
@Table(name = "task_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"task_id", "user_id"})
})
```

---

## 10. 테스트 전략

### 10.1 Entity 단위 테스트
```java
@DisplayName("Task 상태 변경 테스트")
class TaskTest {
    @Test
    void changeStatus_shouldUpdateState() {
        // given/when/then
    }
}
```

### 10.2 Repository/통합 테스트
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class CoreApiIntegrationTest {
    // 로그인/사용자조회/연차승인/특근승인/캘린더/근태 통합 테스트
}
```

---

## 11. 성능 모니터링

### 11.1 쿼리 성능 모니터링
```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/mini_erp
spring.datasource.username=lab
spring.datasource.password=lab
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

## 12. 변경 이력 (현재 반영)
- `request_reason` 컬럼 및 신청 사유 반영
- 직책별 연차 정책(14/16/17/18/19) 반영
- 연차 중복 신청 차단 정책 반영
- `CurrentUserResolver`, `AccessPolicy`, Flyway 마이그레이션 전략 반영
