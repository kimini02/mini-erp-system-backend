package com.minierp.backend.domain.dashboard.service;

import com.minierp.backend.domain.dashboard.dto.DashboardResponseDto;
import com.minierp.backend.domain.project.entity.Project;
import com.minierp.backend.domain.task.entity.Task;
import com.minierp.backend.domain.task.entity.TaskPriority;
import com.minierp.backend.domain.task.entity.TaskStatus;
import com.minierp.backend.domain.task.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = DashboardServiceTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Transactional
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @MockBean
    private TaskRepository taskRepository;

    @Test
    @DisplayName("대시보드 진행률은 상태별 업무 수와 완료율을 계산한다")
    void getDashboardStats_success() {
        given(taskRepository.findAll()).willReturn(List.of(
                createTask(1L, TaskStatus.TODO),
                createTask(2L, TaskStatus.DOING),
                createTask(3L, TaskStatus.DONE),
                createTask(4L, TaskStatus.DONE)
        ));

        DashboardResponseDto response = dashboardService.getDashboardStats();

        assertThat(response.getTodoCount()).isEqualTo(1L);
        assertThat(response.getDoingCount()).isEqualTo(1L);
        assertThat(response.getDoneCount()).isEqualTo(2L);
        assertThat(response.getProgressRate()).isEqualTo(50.0);
    }

    private Task createTask(Long id, TaskStatus taskStatus) {
        Project project = Project.create(
                "ERP 재구축",
                "사내 업무 시스템 고도화",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30)
        );
        ReflectionTestUtils.setField(project, "id", 1L);

        Task task = Task.create(
                "업무 제목",
                "업무 내용",
                LocalDate.of(2026, 4, 2),
                taskStatus,
                TaskPriority.MEDIUM,
                project
        );
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    @TestConfiguration
    static class TestTransactionConfig {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }

    @SpringBootConfiguration
    @Import({DashboardService.class, TestTransactionConfig.class})
    static class TestApplication {
    }
}
