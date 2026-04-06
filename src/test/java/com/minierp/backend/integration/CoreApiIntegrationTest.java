package com.minierp.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minierp.backend.domain.approval.repository.LeaveRequestRepository;
import com.minierp.backend.domain.attendance.repository.AttendanceRepository;
import com.minierp.backend.domain.overtime.repository.OvertimeRequestRepository;
import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.entity.UserRole;
import com.minierp.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class CoreApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private OvertimeRequestRepository overtimeRequestRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    private User adminUser;
    private User leaderUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAllInBatch();
        overtimeRequestRepository.deleteAllInBatch();
        leaveRequestRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        adminUser = User.create(
                "관리소장",
                "manager01",
                "manager01@test.com",
                passwordEncoder.encode("Password123!"),
                "01",
                "관리소장"
        );
        adminUser.changeRole(UserRole.ADMIN);

        leaderUser = User.create(
                "팀장",
                "leader01",
                "leader01@test.com",
                passwordEncoder.encode("Password123!"),
                "02",
                "팀장"
        );
        leaderUser.changeRole(UserRole.TEAM_LEADER);

        normalUser = User.create(
                "일반사용자",
                "user01",
                "user01@test.com",
                passwordEncoder.encode("Password123!"),
                "03",
                "사원"
        );

        userRepository.save(adminUser);
        userRepository.save(leaderUser);
        userRepository.save(normalUser);
    }

    @Test
    @DisplayName("로그인 통합 테스트: Access Token 발급")
    void login_shouldIssueAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "user01",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("사용자 목록 통합 테스트: ADMIN은 목록 조회 가능")
    void getUsers_adminShouldSeeList() throws Exception {
        String adminToken = loginAndGetToken("manager01", "Password123!");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("연차 승인 통합 테스트: USER 신청건을 TEAM_LEADER가 승인")
    void approveLeave_shouldReturnApprovedStatus() throws Exception {
        String userToken = loginAndGetToken("user01", "Password123!");
        String leaderToken = loginAndGetToken("leader01", "Password123!");

        MvcResult createResult = mockMvc.perform(post("/api/v1/leave")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appType": "ANNUAL",
                                  "startDate": "2026-04-08",
                                  "endDate": "2026-04-08",
                                  "requestReason": "병원 진료"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Long appId = readLong(createResult, "data", "appId");

        mockMvc.perform(patch("/api/v1/leave/{requestId}/approve", appId)
                        .header("Authorization", bearer(leaderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.requestReason").value("병원 진료"))
                .andExpect(jsonPath("$.data.approverId").value(leaderUser.getId()));
    }

    @Test
    @DisplayName("특근 승인 통합 테스트: USER 신청건을 TEAM_LEADER가 승인")
    void approveOvertime_shouldReturnApprovedStatus() throws Exception {
        String userToken = loginAndGetToken("user01", "Password123!");
        String leaderToken = loginAndGetToken("leader01", "Password123!");

        MvcResult createResult = mockMvc.perform(post("/api/v1/overtime")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "overtimeDate": "2026-04-11",
                                  "startTime": "09:00:00",
                                  "endTime": "12:00:00",
                                  "reason": "주말 긴급 작업"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Long overtimeId = readLong(createResult, "data", "id");

        mockMvc.perform(patch("/api/v1/overtime/{id}/approve", overtimeId)
                        .header("Authorization", bearer(leaderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approverId").value(leaderUser.getId()));
    }

    @Test
    @DisplayName("캘린더 조회 통합 테스트: 승인된 연차/특근 이벤트가 함께 조회")
    void calendar_shouldContainLeaveAndOvertimeEvents() throws Exception {
        String userToken = loginAndGetToken("user01", "Password123!");
        String leaderToken = loginAndGetToken("leader01", "Password123!");

        Long leaveId = createLeave(userToken, "2026-04-08", "2026-04-08");
        approveLeave(leaderToken, leaveId);

        Long overtimeId = createOvertime(userToken, "2026-04-11");
        approveOvertime(leaderToken, overtimeId);

        mockMvc.perform(get("/api/v1/calendar/events")
                        .header("Authorization", bearer(userToken))
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].type", hasItems("LEAVE", "OVERTIME")));
    }

    @Test
    @DisplayName("근태 체크인/체크아웃 통합 테스트")
    void attendance_checkInAndCheckOut_shouldSucceed() throws Exception {
        String userToken = loginAndGetToken("user01", "Password123!");

        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workDate": "2026-04-09",
                                  "clockInTime": "08:55:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attStatus").value("NORMAL"));

        mockMvc.perform(patch("/api/v1/attendance/check-out")
                        .header("Authorization", bearer(userToken))
                        .param("workDate", "2026-04-09")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clockOutTime": "18:10:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clockOutTime").value("18:10:00"));
    }

    @Test
    @DisplayName("연차 중복 신청 통합 테스트: 같은 날짜 재신청은 거부")
    void createLeave_sameDateDuplicateShouldFail() throws Exception {
        String userToken = loginAndGetToken("user01", "Password123!");

        mockMvc.perform(post("/api/v1/leave")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appType": "ANNUAL",
                                  "startDate": "2026-04-10",
                                  "endDate": "2026-04-10",
                                  "requestReason": "1차 신청"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/leave")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appType": "ANNUAL",
                                  "startDate": "2026-04-10",
                                  "endDate": "2026-04-10",
                                  "requestReason": "중복 신청"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("반차 신청 통합 테스트: 종료일이 자동으로 시작일과 동일하게 보정된다")
    void createHalfDayLeave_shouldNormalizeEndDate() throws Exception {
        String userToken = loginAndGetToken("user01", "Password123!");

        mockMvc.perform(post("/api/v1/leave")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appType": "HALF_MORNING",
                                  "startDate": "2026-04-10",
                                  "endDate": "2026-04-12",
                                  "requestReason": "반차 테스트"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startDate").value("2026-04-10"))
                .andExpect(jsonPath("$.data.endDate").value("2026-04-10"));
    }

    @Test
    @DisplayName("연차 취소 통합 테스트: 본인 PENDING 건은 취소 가능")
    void cancelLeave_shouldSucceedForOwnPendingRequest() throws Exception {
        String userToken = loginAndGetToken("user01", "Password123!");

        MvcResult createResult = mockMvc.perform(post("/api/v1/leave")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appType": "ANNUAL",
                                  "startDate": "2026-04-14",
                                  "endDate": "2026-04-14",
                                  "requestReason": "취소 테스트"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long leaveId = readLong(createResult, "data", "appId");

        mockMvc.perform(patch("/api/v1/leave/{requestId}/cancel", leaveId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appStatus").value("CANCELLED"));
    }

    private Long createLeave(String userToken, String startDate, String endDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/leave")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appType": "ANNUAL",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "requestReason": "테스트 연차"
                                }
                                """.formatted(startDate, endDate)))
                .andExpect(status().isCreated())
                .andReturn();

        return readLong(result, "data", "appId");
    }

    private void approveLeave(String approverToken, Long leaveId) throws Exception {
        mockMvc.perform(patch("/api/v1/leave/{requestId}/approve", leaveId)
                        .header("Authorization", bearer(approverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appStatus").value("APPROVED"));
    }

    private Long createOvertime(String userToken, String overtimeDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/overtime")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "overtimeDate": "%s",
                                  "startTime": "09:00:00",
                                  "endTime": "12:00:00",
                                  "reason": "테스트 특근"
                                }
                                """.formatted(overtimeDate)))
                .andExpect(status().isCreated())
                .andReturn();

        return readLong(result, "data", "id");
    }

    private void approveOvertime(String approverToken, Long overtimeId) throws Exception {
        mockMvc.perform(patch("/api/v1/overtime/{id}/approve", overtimeId)
                        .header("Authorization", bearer(approverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    private String loginAndGetToken(String loginId, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s",
                                  "password": "%s"
                                }
                                """.formatted(loginId, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    private Long readLong(MvcResult mvcResult, String parent, String field) throws Exception {
        JsonNode json = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return json.path(parent).path(field).asLong();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
