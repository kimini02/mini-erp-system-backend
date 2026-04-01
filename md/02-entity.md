# Entity 설계서

## 문서 정보
- **프로젝트명**: 사내 프로젝트 트래킹 및 연차 전자결재 그룹웨어
- **작성자**: 팀 작성
- **작성일**: 2026-03-28
- **버전**: v2.0
- **검토자**: 백엔드 개발팀
- **승인자**: 팀 리더

---

## 1. Entity 설계 개요

### 1.1 설계 목적
> JPA Entity 클래스 설계를 통해 객체-관계 매핑(ORM)을 정의하고,
> 프로젝트 업무 관리/연차 결재 비즈니스 도메인을 코드로 표현하여 유지보수가 용이한 시스템을 구축

### 1.2 설계 원칙
- **단일 책임 원칙**: 하나의 Entity는 하나의 비즈니스 개념만 표현
- **캡슐화**: 핵심 상태 변경 로직을 Entity 내부 메서드로 관리
- **불변성 지향**: 생성 이후 불필요한 Setter 사용 최소화
- **연관관계 최소화**: 필요한 관계만 매핑하여 복잡도와 결합도 감소

### 1.3 기술 스택
- **ORM 프레임워크**: Spring Data JPA 2.7.x
- **데이터베이스**: MariaDB 10.11
- **검증 프레임워크**: Bean Validation 2.0
- **감사 기능**: Spring Data JPA Auditing

---

## 2. Entity 목록 및 분류

### 2.1 Entity 분류 매트릭스
| Entity명 | 유형 | 비즈니스 중요도 | 기술적 복잡도 | 우선순위 |
|----------|------|----------------|---------------|----------|
| **User** | 핵심 | 높음 | 중간 | 1순위 |
| **Task** | 핵심 | 높음 | 중간 | 1순위 |
| **TaskAssignment** | 핵심 | 높음 | 낮음 | 1순위 |
| **Approval(LeaveRequest)** | 핵심 | 높음 | 높음 | 1순위 |
| **Attendance** | 지원 | 중간 | 중간 | 2순위 |

### 2.2 Entity 상속 구조

모든 Entity는 BaseEntity(id, createdAt, updatedAt 포함)를 상속합니다.

---

## 3. 공통 설계 규칙

### 3.1 네이밍 규칙
| 구분 | 규칙 | 예시 | 비고 |
|------|------|------|------|
| **Entity 클래스명** | PascalCase | `User`, `Task` | 단수형 사용 |
| **테이블명** | snake_case | `users`, `tasks` | 복수형 사용 |
| **컬럼명** | snake_case | `user_id`, `task_status` | 언더스코어 구분 |
| **연관관계 필드** | camelCase | `assignedTasks`, `assignee` | 객체 참조명 |
| **boolean 필드** | is + 형용사 | `isActive` | 명확한 의미 |

### 3.2 공통 어노테이션 규칙
```java
// 기본 Entity 구조
@Entity
@Table(name = "테이블명")
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
| **Task** | IDENTITY | 업무 증가 순서 추적 | 1, 2, 3, ... |
| **TaskAssignment** | IDENTITY | 매핑 관리 단순화 | 1, 2, 3, ... |
| **Approval(LeaveRequest)** | IDENTITY | 결재 이력 추적 용이 | 1, 2, 3, ... |
| **Attendance** | IDENTITY | 일자별 기록 관리 용이 | 1, 2, 3, ... |

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
| **id** | `Long` | `user_id` | PK, NOT NULL, AUTO_INCREMENT | 사용자 식별자 | 시스템 자동 생성 |
| **userName** | `String` | `user_name` | NOT NULL, LENGTH(50) | 사용자명 | 2-50자 |
| **userEmail** | `String` | `user_email` | UNIQUE, NOT NULL, LENGTH(100) | 이메일 | 로그인 ID |
| **userPw** | `String` | `user_pw` | NOT NULL, LENGTH(255) | 암호화 비밀번호 | BCrypt 암호화 |
| **positionName** | `String` | `position_name` | NOT NULL, LENGTH(30) | 직책 | 사원/대리/과장/팀장 |
| **assignRole** | `String` | `assign_role` | NULL 허용, LENGTH(30) | 프로젝트 권한 | PM, Member 등 |
| **userRole** | `UserRole` | `user_role` | NOT NULL, ENUM | 역할 | USER, ADMIN |
| **isActive** | `Boolean` | `is_active` | NOT NULL, DEFAULT(true) | 활성 여부 | 소프트 삭제 용도 |

#### 4.1.3 연관관계 매핑
```java
// 1:N - 사용자가 담당자인 업무 목록
@OneToMany(mappedBy = "assignee", fetch = FetchType.LAZY)
private List<Task> assignedTasks = new ArrayList<>();

// 1:N - 사용자가 업무에 할당된 목록
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
private List<TaskAssignment> taskAssignments = new ArrayList<>();

// 1:N - 사용자가 신청한 결재 목록
@OneToMany(mappedBy = "requester", fetch = FetchType.LAZY)
private List<Approval> leaveRequests = new ArrayList<>();
```

### 4.2 Project Entity 
### (업무(Task)의 상위 개념으로서 프로젝트를 관리할 수 있도록 설계)

#### 4.2.1 기본 정보
@Entity
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Project extends BaseEntity {
    // 필드 정의 (아래 상세 명세 참조)
}


#### 4.2.2 필드 상세 명세 
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|---------------|
| **id** | `Long` | `project_id` | PK, NOT NULL, AUTO_INCREMENT | 프로젝트 식별자 | 시스템 자동 생성 |
| **title** | `String` | `title` | NOT NULL, LENGTH(100) | 프로젝트 명칭 | 필수 입력 |
| **content** | `String` | `content` | LENGTH(1000) | 프로젝트 내용 | 프로젝트 상세 설명 |
| **startDate** | `LocalDate` | `start_date` | NOT NULL | 시작 일자 |필수 입력 |
| **endDate** | `LocalDate` | `end_date` | NOT NULL | 종료 일자 | 시작일 이후여야 함 |
| **status** | `ProjectStatus` | `status` | NOT NULL, ENUM | 진행 상태| READY, PROGRESS, DONE, HOLD |


#### 4.1.3 구현 코드 예시 및 연관관계 매핑
@Entity
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String content;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.READY;

    // 1:N - 프로젝트에 포함된 업무(Task) 목록
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void validatePeriod() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
    }
}

```


```
### 4.3 Task Entity

#### 4.3.1 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|----------------|
| **id** | `Long` | `id` | PK, NOT NULL, AUTO_INCREMENT | 업무 식별자 | 시스템 자동 생성 |
| **taskTitle** | `String` | `Task_title` | NOT NULL, LENGTH(100) | 업무 제목 | 필수 입력 |
| **taskContent** | `String` | `Task_content` | NOT NULL, LENGTH(1000) | 업무 내용 | 필수 입력 |
| **taskState** | `TaskStatus` | `Task_State` | NOT NULL, ENUM | 진행 상태 | TODO, DOING, DONE |
| **startDate** | `LocalDate` | `start_date` | NOT NULL | 시작 일자 | 필수 입력 |
| **endDate** | `LocalDate` | `end_date` | NOT NULL | 종료 일자 | 시작일 이후 |
| **taskNo** | `String` | `Task_No` | NULL 허용, LENGTH(50) | 업무 번호 | 업무 분류 번호 |

#### 4.3.2 구현 코드 예시
```java
@Entity
@Table(name = "tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Task extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;

    @Column(name = "Task_title", nullable = false, length = 100)
    @NotBlank
    private String taskTitle;

    @Column(name = "Task_content", nullable = false, length = 1000)
    @NotBlank
    private String taskContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "Task_State", nullable = false)
    private TaskStatus taskState = TaskStatus.TODO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "Task_No", length = 50)
    private String taskNo;

    public void changeStatus(TaskStatus nextStatus, Long actorUserId, UserRole actorRole) {
        if (actorRole == UserRole.USER && !assignee.getId().equals(actorUserId)) {
            throw new IllegalStateException("본인 Task만 상태 변경할 수 있습니다");
        }
        this.taskState = nextStatus;
    }

    @PrePersist
    @PreUpdate
    private void validatePeriod() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다");
        }
    }
}


```

### 4.4 TaskAssignment Entity

#### 4.4.1 기본 정보
```java
@Entity
@Table(name = "task_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"task_id", "user_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TaskAssignment extends BaseEntity {
    // 필드 정의 (아래 상세 명세 참조)
}
```

#### 4.4.2 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|----------------|
| **id** | `Long` | `id` | PK, NOT NULL, AUTO_INCREMENT | 기본키 | 시스템 자동 생성 |
| **task** | `Task` | `task_id` | FK, NOT NULL | 업무 FK | 필수 참조 |
| **user** | `User` | `user_id` | FK, NOT NULL | 담당자 ID | 필수 참조 |

#### 4.4.3 연관관계 매핑
```java
@Entity
@Table(name = "task_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"task_id", "user_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TaskAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
```

---

### 4.5 Approval Entity (LeaveRequest 테이블)

#### 4.5.1 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|---------------|
| **appId** | `Long` | `app_id` | PK, NOT NULL, AUTO_INCREMENT | 결재 식별자 | 시스템 자동 생성 |
| **requesterId** | `Long` | `requester_id` | FK, NOT NULL | 기안자(신청자) ID | 필수 참조 |
| **approverId** | `Long` | `approver_id` | FK | 결재자(승인자) ID | 처리 시 입력 |
| **appType** | `LeaveType` | `app_type` | NOT NULL, ENUM | 결재 유형 | ANNUAL(연차), HALF(반차), SICK(병가) |
| **startDate** | `LocalDate` | `start_date` | NOT NULL | 휴가 시작일 | 필수 입력 |
| **endDate** | `LocalDate` | `end_date` | NOT NULL | 휴가 종료일 | 시작일 이후 |
| **usedDays** | `BigDecimal` | `used_days` | NOT NULL | 실제 차감 일수 | 주말/공휴일을 제외한 순수 평일 합계. (반차는 0.5로 고정 계산) |
| **appStatus** | `LeaveStatus` | `app_status` | NOT NULL, ENUM | 결재 상태 | PENDING(대기), APPROVED(승인), REJECTED(반려) |
| **rejectReason** | `String` | `reject_reason` | LENGTH(500) | 반려 사유 | 반려 시 관리자가 입력 |
| **createdAt** | `LocalDateTime` | `created_at` | NOT NULL | 기안 일시 | 자동 생성 |

#### 4.5.2 구현 코드 예시
```java
@Entity
@Table(name = "leave_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Column(name = "app_type", nullable = false, length = 50)
    private LeaveType appType;  // ANNUAL, HALF, SICK

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "used_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal usedDays;

    @Column(name = "app_status", nullable = false, length = 50)
    private LeaveStatus appStatus = LeaveStatus.PENDING;  // PENDING, APPROVED, REJECTED

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    public void approve(User approver) {
        if (!"PENDING".equals(this.appStatus)) {
            throw new IllegalStateException("승인 가능한 상태가 아닙니다");
        }
        this.approver = approver;
        this.appStatus = LeaveStatus.APPROVED;
    }

    public void reject(User approver, String reason) {
        if (this.appStatus == LeaveStatus.PENDING) {
            throw new IllegalStateException("반려 가능한 상태가 아닙니다");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("반려 사유는 필수입니다");
        }
        this.approver = approver;
        this.rejectReason = reason;
        this.appStatus = LeaveStatus.REJECTED;
    }

    /**
     * 주말을 제외한 실제 연차 소진 일수를 계산하여 usedDays를 설정하는 메서드 추가
     * @param holidayList 공휴일 정보 (Service에서 넘겨줌)
     */
    public void calculateUsedDays(List<LocalDate> holidayList) {
        long workingDays = startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY)
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SUNDAY)
                .filter(date -> !holidayList.contains(date))
                .count();

        if (workingDays == 0) {
            throw new IllegalArgumentException("신청 기간에 평일이 포함되어 있지 않습니다.");
        }
        
        // 반차(HALF)인 경우 0.5, 연차(ANNUAL)인 경우 계산된 일수 적용
        this.usedDays = (this.appType == LeaveType.HALF) 
                        ? new BigDecimal("0.5") 
                        : BigDecimal.valueOf(workingDays);
    }

    @PrePersist
    @PreUpdate
    private void validatePeriod() {
        // 기존 시작일/종료일 검증 로직 유지
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
        // [추가] 시작일이나 종료일 자체가 주말인 경우 UI와 별개로 서버에서 한 번 더 차단
        if (isWeekend(startDate) || isWeekend(endDate)) {
            throw new IllegalArgumentException("주말은 연차 신청이 불가능합니다.");
        }
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
```

### 4.6 Attendance Entity(근태 및 연차 테이블)

#### 4.6.1 필드 상세 명세
| 필드명 | 데이터 타입 | 컬럼명 | 제약조건 | 설명 | 비즈니스 규칙 |
|--------|-------------|--------|----------|------|---------------|
| **attId** | `Long` | `att_id` | PK, NOT NULL, AUTO_INCREMENT | 근태 식별자 | 시스템 자동 생성 |
| **userId** | `Long` | `user_id` | FK, NOT NULL | 사용자 ID | 필수 참조 |
| **workDate** | `LocalDate` | `work_date` | NOT NULL | 총 근무 일자 | 사용자+근무일 유일 |
| **clockInTime** | `LocalTime` | `clock_in_time` | NULL 허용 | 출근 시간 | 옵션 |
| **clockOutTime** | `LocalTime` | `clock_out_time` | NULL 허용 | 퇴근 시간 | 옵션 |
| **attStatus** | `AttendanceStatus` | `att_status` | NOT NULL, ENUM | 근태 상태 | NORMAL, LATE, ABSENT, LEAVE |
| **totalAnnualLeave** | `BigDecimal` | `total_annual_leave` | NOT NULL, DEFAULT(0) | 총 연차 일수 | 직급 기준 부여 |
| **usedAnnualLeave** | `BigDecimal` | `used_annual_leave` | NOT NULL, DEFAULT(0) | 연차 남은 일수 | 0 이상 |
| **remainingAnnualLeave** | `BigDecimal` | `remaining_annual_leave` | NOT NULL, DEFAULT(0) | 연차 사용 일수 | 총 - 남은 일수 |
| **usedDays** | `BigDecimal` | `used_days` | NULL 허용 | 해당 결재로 소모되는 일수 | LeaveRequest의 usedDays와 동기화. 결재 승인 완료 시점에 해당 값만큼 연차 차감 로직 실행 |

#### 4.6.2 구현 코드 예시
```java
@Entity
@Table(name = "attendances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "work_date"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "clock_in_time")
    private LocalTime clockInTime;

    @Column(name = "clock_out_time")
    private LocalTime clockOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "att_status", nullable = false)
    private AttendanceStatus attStatus = AttendanceStatus.NORMAL;

    @Column(name = "total_annual_leave", nullable = false, precision = 4, scale = 1)
    private BigDecimal totalAnnualLeave = BigDecimal.ZERO;

    @Column(name = "used_annual_leave", nullable = false, precision = 4, scale = 1)
    private BigDecimal usedAnnualLeave = BigDecimal.ZERO;

    @Column(name = "remaining_annual_leave", nullable = false, precision = 4, scale = 1)
    private BigDecimal remainingAnnualLeave = BigDecimal.ZERO;

    @Column(name = "used_days", precision = 4, scale = 1)
    private BigDecimal usedDays;

    public void deductAnnualLeave(BigDecimal days) {
        if (remainingAnnualLeave.compareTo(days) < 0) {
            throw new IllegalArgumentException("잔여 연차가 부족합니다");
        }
        this.usedAnnualLeave = usedAnnualLeave.add(days);
        this.remainingAnnualLeave = remainingAnnualLeave.subtract(days);
        this.usedDays = days;
    }

    public void restoreAnnualLeave(BigDecimal days) {
        this.usedAnnualLeave = usedAnnualLeave.subtract(days);
        this.remainingAnnualLeave = remainingAnnualLeave.add(days);
    }
/**
     * 출근 기록 생성 시 검증 로직
     * @param isHoliday 해당 날짜가 주말/공휴일인지 여부
     * @param hasApprovedOvertime 승인된 특근 신청서 존재 여부
     */
    public void checkIn(LocalTime now, boolean isHoliday, boolean hasApprovedOvertime) {
        // [요구사항 반영] 주말/공휴일인데 승인된 특근이 없는 경우
        if (isHoliday && !hasApprovedOvertime) {
            throw new IllegalStateException("승인된 특근 신청서가 없어 출근 처리가 불가능합니다.");
        }

        this.clockInTime = now;
        
        // 지각 여부 등 기존 로직 처리
        if (now.isAfter(LocalTime.of(9, 0))) {
            this.attStatus = AttendanceStatus.LATE;
        } else {
            this.attStatus = AttendanceStatus.NORMAL;
        }
    }
}
```

---

## 5. Enum 타입 정의

### 5.1 UserRole
```java
public enum UserRole {
    USER("일반 사용자"),
    ADMIN("관리자");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### 5.2 TaskStatus
```java
public enum TaskStatus {
    TODO("할 일"),
    DOING("진행 중"),
    DONE("완료");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean isFinished() {
        return this == DONE;
    }
}
```

### 5.3 LeaveStatus
```java
public enum LeaveStatus {
    PENDING("결재 대기"),
    APPROVED("승인"),
    REJECTED("반려");

    private final String displayName;

    LeaveStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean isClosed() {
        return this == APPROVED || this == REJECTED;
    }
}
```

### 5.4 LeaveType
```java
public enum LeaveType {
    ANNUAL("연차", new BigDecimal("1.0")),
    HALF("반차", new BigDecimal("0.5"));

    private final String displayName;
    private final BigDecimal unitDays;

    LeaveType(String displayName, BigDecimal unitDays) {
        this.displayName = displayName;
        this.unitDays = unitDays;
    }

    public BigDecimal getUnitDays() {
        return unitDays;
    }
}
```

### 5.5 AttendanceStatus
```java
public enum AttendanceStatus {
    NORMAL("정상 출근"),
    LATE("지각"),
    ABSENT("결근"),
    LEAVE("휴가");

    private final String displayName;

    AttendanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

---

## 6. 연관관계 매핑 전략

### 6.1 연관관계 매핑 규칙
| 관계 유형 | 기본 전략 | 이유 | 예외 상황 |
|----------|-----------|------|-----------|
| **@ManyToOne** | LAZY | 성능 최적화 | 필수 조회는 Fetch Join으로 해결 |
| **@OneToMany** | LAZY | N+1 문제 방지 | 소량 데이터만 제한적 조회 |
| **@OneToOne** | LAZY | 일관성 유지 | 도메인상 강결합일 때 EAGER 검토 |
| **@ManyToMany** | 사용 금지 | 복잡성 증가 | 중간 Entity(`ProjectMember`)로 대체 |

### 6.2 Cascade 옵션 가이드
```java
// 부모-자식 관계 (강한 연결)
@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ProjectMember> members = new ArrayList<>();

// 참조 관계 (약한 연결)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "assignee_id", nullable = false)
private User assignee; // cascade 없음
```

### 6.3 양방향 연관관계 관리
```java
// Project Entity에서
public void addMember(ProjectMember member) {
    members.add(member);
    member.linkProject(this);
}

public void addTask(Task task) {
    tasks.add(task);
    task.linkProject(this);
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
    @Column(name = "id")
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

### 7.2 Auditing 설정
```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("SYSTEM");
            }
            return Optional.of(authentication.getName());
        };
    }
}
```

---

## 8. 성능 최적화 전략

### 8.1 N+1 문제 해결
```java
// Repository에서 fetch join 사용
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @Query("SELECT lr FROM LeaveRequest lr " +
           "JOIN FETCH lr.requester r " +
           "LEFT JOIN FETCH lr.approver a " +
           "WHERE lr.status = :status")
    List<LeaveRequest> findByStatusWithUsers(@Param("status") LeaveStatus status);

    @EntityGraph(attributePaths = {"project", "assignee"})
    List<Task> findByProjectId(Long projectId);
}
```

### 8.2 쿼리 최적화
```java
// 페이징과 정렬
@Query("SELECT t FROM Task t " +
       "WHERE t.assignee.id = :userId " +
       "ORDER BY t.createdAt DESC")
Page<Task> findMyTasksOrderByCreatedAtDesc(
    @Param("userId") Long userId,
    Pageable pageable);
```

---

## 9. 검증 및 제약조건

### 9.1 Bean Validation 어노테이션
```java
// Entity에서 사용
@Email(message = "유효한 이메일 형식이어야 합니다")
@Column(nullable = false, unique = true, length = 100)
private String email;
```

### 9.2 데이터베이스 제약조건
```java
@Entity
@Table(name = "leave_requests")
public class LeaveRequest extends BaseEntity {

    @PrePersist
    @PreUpdate
    private void validateConstraints() {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다");
        }
        if (status == LeaveStatus.REJECTED && (rejectReason == null || rejectReason.isBlank())) {
            throw new IllegalArgumentException("반려 사유는 필수입니다");
        }
    }
}
```

---

## 10. 테스트 전략

### 10.1 Entity 단위 테스트
```java
@DisplayName("LeaveRequest Entity 테스트")
class LeaveRequestTest {

    @Test
    @DisplayName("반려 상태 전환 시 반려 사유가 없으면 예외가 발생해야 한다")
    void rejectWithoutReason_ShouldThrowException() {
        // given
        LeaveRequest request = createPendingRequest();
        User approver = createAdminUser();

        // when & then
        assertThatThrownBy(() -> request.reject(approver, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("반려 사유");
    }
}
```

### 10.2 Repository 테스트
```java
@DataJpaTest
@DisplayName("TaskRepository 테스트")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("담당자 ID로 내 업무를 페이징 조회해야 한다")
    void findMyTasksOrderByCreatedAtDesc_ShouldReturnPagedTasks() {
        // given
        Long userId = 1L;

        // when
        Page<Task> page = taskRepository.findMyTasksOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 10));

        // then
        assertThat(page).isNotNull();
    }
}
```

---

## 11. 성능 모니터링

### 11.1 쿼리 성능 모니터링
```properties
# application.properties - 쿼리 성능 모니터링 설정

# DB 접속정보
spring.datasource.url=jdbc:mariadb://localhost:3306/mini_erp
spring.datasource.username=lab
spring.datasource.password=lab
spring.datasource.driverClassName=org.mariadb.jdbc.Driver

# JPA 및 Hibernate 설정
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.SQL=debug
logging.level.org.hibernate.orm.jdbc.bind=trace
```

---

## 12. 0번 요구사항 반영 추가사항

### 12.1 권한 요구사항 기반 엔티티 제약
- `Task.changeStatus()`는 USER의 경우 `assignee == 로그인 사용자` 검증 필수
- `Project` 조회 서비스는 USER 권한에서 `ProjectMember` 기반 본인 참여 프로젝트만 반환
- `LeaveRequest` 조회 서비스는 USER 권한에서 `requester == 로그인 사용자` 조건 강제

### 12.2 핵심 비즈니스 규칙 기반 DB 제약 강화
- `ProjectMember(project_id, user_id)` 유니크 제약 필수
- `Task` 생성 시 `assignee`가 해당 프로젝트 참여자인지 애플리케이션 레벨 검증 필수
- `LeaveRequest(status=REJECTED)` 상태 전환 시 `reject_reason` NOT BLANK 검증 필수
- 동일한 날짜에 중복으로 연차를 신청할 수 없도록 하는 DB/애플리케이션 제약 조건

### 12.3 연차 승인 차감 트랜잭션 규칙 반영
- 승인 처리 메서드와 `User.remainingAnnualLeave` 차감은 하나의 서비스 트랜잭션으로 처리
- 차감 실패 시 `LeaveRequest.status=APPROVED` 반영도 롤백되어야 함

### 12.4 캘린더/근태 요구사항 반영 메모
- `Attendance`는 조회 중심 엔티티로 유지
- 실제 출퇴근 기록 기능 포함 확정 시 아래 필드/메서드 활성화
    - `clockInTime`, `clockOutTime`
    - `checkIn()`, `checkOut()`

### 12.5 MVP 포함 기능 엔티티 매핑표
| MVP 기능 | 주요 Entity |
|---|---|
| 회원가입/로그인 | `User` |
| 프로젝트 생성/조회 | `Project` |
| 프로젝트 참여자 배정 | `ProjectMember` |
| Task 생성/상태 변경 | `Task` |
| 연차 신청/승인/반려 | `LeaveRequest` |
| 승인 시 연차 차감 | `LeaveRequest`, `User` |
| 업무 진행도 확인 | `Project`, `Task` |

---

**문서 끝**