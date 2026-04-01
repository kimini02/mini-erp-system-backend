# 김민희 - 프로젝트/Task 파트

## 담당 도메인
- `domain/project/` (프로젝트 생성/조회, 참여자 배정)
- `domain/task/` (Task 생성/조회/상태변경, 담당자 배정)
- `domain/dashboard/` (진행률/통계)

---

## 해야 할 일 요약

### 1순위: Project CRUD + 참여자 배정
| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/v1/projects` | 프로젝트 생성 | ADMIN만 |
| `GET /api/v1/projects` | 프로젝트 목록 조회 | USER는 본인 배정 프로젝트만 |
| `POST /api/v1/projects/{projectId}/members` | 팀원 배정 | ADMIN만, 중복 409 |
| `DELETE /api/v1/projects/{projectId}/members/{userId}` | 팀원 해제 | ADMIN만 |

### 2순위: Task CRUD + 상태변경
| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/v1/tasks` | Task 생성 | ADMIN만, assigneeIds로 다중 담당자 |
| `GET /api/v1/tasks` | Task 목록 조회 | USER는 본인 배정 Task만 |
| `GET /api/v1/tasks/{taskId}` | Task 상세 조회 | USER/ADMIN |
| `PATCH /api/v1/tasks/{taskId}/status` | 상태 변경 | USER는 본인 Task만 |

### 3순위: Task 담당자 관리
| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/v1/tasks/{taskId}/assignments` | 담당자 배정 | ADMIN만, 중복 409 |
| `GET /api/v1/tasks/{taskId}/assignments` | 담당자 목록 조회 | USER/ADMIN |
| `DELETE /api/v1/tasks/{taskId}/assignments/{userId}` | 담당자 해제 | ADMIN만 |

### 4순위: 진행률/통계
| API | 메서드 | 설명 |
|---|---|---|
| `GET /api/v1/projects/{projectId}/progress` | 프로젝트 진행률 | DONE Task / 전체 Task |
| `GET /api/v1/dashboard/progress` | 대시보드 통계 | TODO/DOING/DONE 전체 통계 |

---

## 구현 시 주의사항
- Task 담당자는 **TaskAssignment 기반 다중 담당자만** 사용 (단일 assignee 혼용 금지)
- USER는 **본인이 배정된 프로젝트/Task만** 조회 가능
- Task 상태 변경 시 USER는 본인 Task만 가능 (ADMIN은 전체)
- start_date < end_date 검증 필수 (Project, Task 둘 다)
- 진행률 = (DONE Task 수 / 전체 Task 수) * 100

---

## 작업 파일 목록
```
domain/project/entity/Project.java
domain/project/entity/ProjectStatus.java
domain/project/controller/ProjectController.java
domain/project/service/ProjectService.java
domain/project/repository/ProjectRepository.java
domain/project/dto/ProjectCreateRequestDto.java
domain/project/dto/ProjectResponseDto.java
domain/task/entity/Task.java
domain/task/entity/TaskAssignment.java
domain/task/entity/TaskStatus.java
domain/task/controller/TaskController.java
domain/task/service/TaskService.java
domain/task/repository/TaskRepository.java
domain/task/repository/TaskAssignmentRepository.java
domain/task/dto/TaskCreateRequestDto.java
domain/task/dto/TaskResponseDto.java
domain/task/dto/TaskStatusUpdateDto.java
domain/dashboard/controller/DashboardController.java
domain/dashboard/service/DashboardService.java
domain/dashboard/dto/DashboardResponseDto.java
```
