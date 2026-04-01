# CLAUDE.md — SmartWork BE 공통

> 이 프로젝트에서 Claude가 코드를 작성할 때 **반드시** 따라야 하는 규칙.
> 팀원 3명(김보민, 김민희, 이세연)의 코드 스타일을 통일하기 위한 문서.

---

## 필수 읽기 문서

코드 작성 전에 아래 문서를 반드시 읽는다:

| 문서 | 경로 | 내용 |
|------|------|------|
| 공통 코딩 규칙 | `md/00-coding-convention.md` | Entity/DTO/Controller/Service/Repository 규칙 전체 |
| 도메인 설계서 | `md/domain.md` | 비즈니스 규칙, 도메인 관계, 용어 정의 |
| Entity 설계서 | `md/Entity.md` | 필드/연관관계/제약조건 상세 명세 |
| REST API 설계서 | `md/RESTAPI.md` | URL/메서드/상태코드/요청응답 형식 |

---

## 코딩 규칙 요약 (상세는 00-coding-convention.md)

### Entity
- `@Setter` 금지, `@Data` 금지
- `BaseEntity` 상속 (id, createdAt, updatedAt)
- 정적 팩토리 메서드 `create()` / `of()`로 생성
- 상태 변경은 의미 있는 메서드명 (`approve()`, `changeStatus()`)
- `FetchType.LAZY` 통일, `@Enumerated(EnumType.STRING)` 통일

### DTO
- Request / Response 분리
- Response DTO에 `from(Entity)` 정적 메서드 필수
- Request DTO에 Bean Validation 어노테이션 필수

### Controller
- 비즈니스 로직 금지, Service에 위임만
- `@Valid` 필수
- 반환: `ResponseEntity<ApiResponse<T>>`

### Service
- 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기만 `@Transactional`
- 생성자 주입: `@RequiredArgsConstructor` + `private final`
- 조회 실패: `throw new BusinessException(ErrorCode.XXX_NOT_FOUND)`

### Repository
- `JpaRepository` 상속
- 단순 조건은 메서드명 쿼리, 복잡한 것만 `@Query` JPQL

### 예외 처리
- `BusinessException(ErrorCode.XXX)` 통일
- 도메인별 커스텀 Exception 만들지 않는다
- `GlobalExceptionHandler`가 일괄 처리

---

## 테스트 필수

기능을 구현하면 반드시 테스트도 함께 작성한다.

| 대상 | 방식 | 필수 |
|------|------|------|
| Entity | 순수 단위 테스트 (Spring 없이) | **필수** |
| Service | `@SpringBootTest` + `@Transactional` 통합 테스트 | **필수** |
| Controller | `@WebMvcTest` | 선택 |

테스트 네이밍: `@DisplayName("한글 설명")` + given-when-then 패턴

---

## 기록 필수

### 매 작업 완료 시
- 각자 개인 폴더(`md/kimminhee/`, 등)의 daily-log에 기록
- 커밋 메시지 형식: `타입(범위): 설명`

### 에러/버그 해결 시
- troubleshooting에 기록 (상황 → 원인 → 해결 → 교훈)

### 기능 구현 완료 시
- next-steps에서 해당 항목 체크
- 노션에 동기화

---

## 작업 전 체크리스트

```
코드 작성 전:
  [ ] 00-coding-convention.md 규칙 확인
  [ ] Entity.md에서 해당 Entity 스펙 확인
  [ ] RESTAPI.md에서 해당 API 스펙 확인

코드 작성 후:
  [ ] 테스트 코드 작성 완료
  [ ] 테스트 통과 확인
  [ ] daily-log 기록
  [ ] 에러 있었으면 troubleshooting 기록
  [ ] 커밋 메시지 형식 준수
```

---

## 팀 분업

| 담당자 | 도메인 | 파트 문서 |
|--------|--------|----------|
| 김보민 | `global/`, `domain/user/` | `md/part-kimbomin.md` |
| 김민희 | `domain/project/`, `domain/task/`, `domain/dashboard/` | `md/kimminhee/part-kimminhee.md` |
| 이세연 | `domain/approval/`, `domain/attendance/`, `domain/calendar/` | `md/part-leeseyeon.md` |

다른 사람 담당 패키지를 수정할 때는 반드시 해당 담당자와 합의 후 진행.

---

## 기술 스택

- Java 17 / Spring Boot 3.5.13 / MariaDB 10.11
- Spring Data JPA / Spring Security / JWT Access Token only
- Maven / Flyway / springdoc-openapi (Swagger)
- Lombok (`@Getter`, `@NoArgsConstructor`, `@RequiredArgsConstructor` 만 사용)
