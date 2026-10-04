package com.bugtracker.integration;

import com.bugtracker.entity.RoleName;
import com.bugtracker.entity.User;
import com.bugtracker.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP tests: real Spring context, real JWT filter chain and the H2
 * database configured in {@code application-test.properties}. Each MockMvc call
 * runs in its own transaction and commits, exactly like a production request, so
 * the tables are wiped in {@link #setUp()} before the fixture accounts are
 * created.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BugTrackerApiIntegrationTest {

    private static final String ADMIN_PASSWORD = "Admin@12345";
    private static final String TESTER_PASSWORD = "Tester@12345";
    private static final String DEVELOPER_PASSWORD = "Developer@12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;
    private User tester;
    private User developer;
    private User otherDeveloper;

    private String adminToken;
    private String testerToken;
    private String developerToken;
    private String otherDeveloperToken;

    @BeforeEach
    void setUp() throws Exception {
        wipeDatabase();

        admin = userService.createUser("Ada Admin", ADMIN_PASSWORD, "admin@integration.test", RoleName.ROLE_ADMIN);
        tester = userService.createUser("Tom Tester", TESTER_PASSWORD, "tester@integration.test", RoleName.ROLE_TESTER);
        developer = userService.createUser("Dev One", DEVELOPER_PASSWORD, "developer@integration.test",
                RoleName.ROLE_DEVELOPER);
        otherDeveloper = userService.createUser("Dev Two", DEVELOPER_PASSWORD, "developer2@integration.test",
                RoleName.ROLE_DEVELOPER);

        adminToken = login("admin@integration.test", ADMIN_PASSWORD);
        testerToken = login("tester@integration.test", TESTER_PASSWORD);
        developerToken = login("developer@integration.test", DEVELOPER_PASSWORD);
        otherDeveloperToken = login("developer2@integration.test", DEVELOPER_PASSWORD);
    }

    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /**
     * Removes every row written by earlier tests, children first so that foreign
     * keys stay satisfied.
     */
    private void wipeDatabase() {
        jdbcTemplate.update("DELETE FROM attachments");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM bug_history");
        jdbcTemplate.update("DELETE FROM bugs");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM sequences");
    }
    /**
     * Creates a bug through the public API and returns its identifier.
     */
    private long createBug(String token, String title, Long assignedDeveloperId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", "Reported by the integration test suite.");
        payload.put("stepsToReproduce", "1. Open the page\n2. Observe the failure");
        payload.put("expectedResult", "The page renders correctly.");
        payload.put("actualResult", "The page fails to render.");
        payload.put("environment", "Chrome 124 / Windows 11");
        payload.put("severity", "HIGH");
        payload.put("priority", "MEDIUM");
        payload.put("assignedDeveloperId", assignedDeveloperId);

        String response = mockMvc.perform(post("/api/bugs")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.bugCode").value(matchesPattern("BUG-\\d+")))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("id").asLong();
    }

    private ResultActions changeStatus(String token, long bugId, String status, String resolution) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("resolution", resolution);
        return mockMvc.perform(patch("/api/bugs/{id}/status", bugId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    private ResultActions changeSeverity(String token, long bugId, String severity) throws Exception {
        return mockMvc.perform(patch("/api/bugs/{id}/severity", bugId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("severity", severity))));
    }

    private ResultActions changePriority(String token, long bugId, String priority) throws Exception {
        return mockMvc.perform(patch("/api/bugs/{id}/priority", bugId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("priority", priority))));
    }

    private ResultActions assignBug(String token, long bugId, Long developerId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assignedDeveloperId", developerId);
        return mockMvc.perform(patch("/api/bugs/{id}/assign", bugId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    @Test
    @DisplayName("registration returns a bearer token that unlocks protected endpoints")
    void registrationIssuesUsableToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "New Tester",
                                "email", "new.tester@integration.test",
                                "password", "Passw0rd1",
                                "role", "ROLE_TESTER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.role").value("ROLE_TESTER"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).path("token").asText();
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.tester@integration.test"))
                .andExpect(jsonPath("$.fullName").value("New Tester"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("self registration as ADMIN is rejected and input is validated")
    void registrationRejectsPrivilegeEscalationAndBadInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Sneaky Admin",
                                "email", "sneaky@integration.test",
                                "password", "Passw0rd1",
                                "role", "ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Self registration as ADMIN is not permitted. Please contact an administrator."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Copy Cat",
                                "email", "tester@integration.test",
                                "password", "Passw0rd1",
                                "role", "ROLE_TESTER"))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Weak Password",
                                "email", "weak@integration.test",
                                "password", "onlyletters",
                                "role", "ROLE_TESTER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("anonymous calls are 401 and wrong credentials are 401")
    void unauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/bugs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/statistics")).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "admin@integration.test",
                                "password", "NotThePassword1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @DisplayName("user directory and role changes are administrator only")
    void userAdministrationIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].email").isNotEmpty())
                .andExpect(jsonPath("$[0].role").isNotEmpty());

        mockMvc.perform(get("/api/users").header("Authorization", bearer(developerToken)))
                .andExpect(status().isForbidden());

        // Every signed-in user may read the assignee shortlist (developers + administrators).
        mockMvc.perform(get("/api/users/assignable").header("Authorization", bearer(testerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(patch("/api/users/{id}/role", developer.getId())
                        .header("Authorization", bearer(testerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ROLE_TESTER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/users/{id}/role", developer.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ROLE_TESTER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(developer.getId().intValue()))
                .andExpect(jsonPath("$.role").value("ROLE_TESTER"));

        mockMvc.perform(patch("/api/users/{id}/status", otherDeveloper.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // A disabled account can no longer sign in.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "developer2@integration.test",
                                "password", DEVELOPER_PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("bug lifecycle enforces the transition matrix and role permissions")
    void bugLifecycleEnforcesWorkflowAndPermissions() throws Exception {
        long bugId = createBug(testerToken, "Checkout button is unresponsive", null);

        // Testers create and triage bugs but may only verify (close/reopen) a resolved bug.
        changeStatus(testerToken, bugId, "IN_PROGRESS", null).andExpect(status().isForbidden());

        // Only administrators may assign work.
        assignBug(testerToken, bugId, developer.getId()).andExpect(status().isForbidden());
        assignBug(adminToken, bugId, developer.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedDeveloperId").value(developer.getId().intValue()))
                .andExpect(jsonPath("$.assignedDeveloperName").value("Dev One"));

        // A developer who is not the assignee may not move the ticket.
        changeStatus(otherDeveloperToken, bugId, "IN_PROGRESS", null).andExpect(status().isForbidden());

        changeStatus(developerToken, bugId, "IN_PROGRESS", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Resolving requires a resolution summary.
        changeStatus(developerToken, bugId, "RESOLVED", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A resolution summary is required to resolve a bug."));
        changeStatus(developerToken, bugId, "RESOLVED", "Guarded the null session lookup.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolution").value("Guarded the null session lookup."));

        // A tester verifies the fix and closes the bug.
        changeStatus(testerToken, bugId, "CLOSED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // Closed bugs can only be reopened.
        changeStatus(adminToken, bugId, "IN_PROGRESS", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid status transition from CLOSED to IN_PROGRESS."));

        changeStatus(adminToken, bugId, "REOPENED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"));

        // A reopened, unresolved bug is outside the tester's verification window.
        changeStatus(testerToken, bugId, "CLOSED", null).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/bugs/{id}", bugId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporterName").value("Tom Tester"))
                .andExpect(jsonPath("$.history.length()").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.comments.length()").value(0))
                .andExpect(jsonPath("$.attachments.length()").value(0));
    }

    @Test
    @DisplayName("triage fields are tester/admin only and deletion is administrator only")
    void triageAndDeletePermissions() throws Exception {
        long bugId = createBug(testerToken, "Dark mode text is unreadable", null);

        changeSeverity(developerToken, bugId, "CRITICAL").andExpect(status().isForbidden());
        changePriority(developerToken, bugId, "URGENT").andExpect(status().isForbidden());

        changeSeverity(testerToken, bugId, "CRITICAL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("CRITICAL"));
        changeSeverity(testerToken, bugId, "CRITICAL")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bug already has severity CRITICAL."));

        changePriority(testerToken, bugId, "URGENT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT"));

        mockMvc.perform(delete("/api/bugs/{id}", bugId).header("Authorization", bearer(testerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/bugs/{id}", bugId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/bugs/{id}", bugId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("comments and attachments round-trip with author/uploader permissions")
    void commentsAndAttachmentsRoundTrip() throws Exception {
        long bugId = createBug(testerToken, "Attachment upload fails on Safari", null);

        String commentResponse = mockMvc.perform(post("/api/bugs/{id}/comments", bugId)
                        .header("Authorization", bearer(developerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Reproduced on build 42."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Dev One"))
                .andExpect(jsonPath("$.content").value("Reproduced on build 42."))
                .andReturn().getResponse().getContentAsString();
        long commentId = objectMapper.readTree(commentResponse).path("id").asLong();

        mockMvc.perform(get("/api/bugs/{id}/comments", bugId).header("Authorization", bearer(testerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Only the author (or an administrator) may delete a comment.
        mockMvc.perform(delete("/api/bugs/{id}/comments/{commentId}", bugId, commentId)
                        .header("Authorization", bearer(testerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/bugs/{id}/comments/{commentId}", bugId, commentId)
                        .header("Authorization", bearer(developerToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/bugs/{id}/comments", bugId).header("Authorization", bearer(testerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        byte[] png = "fake-png-bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile evidence = new MockMultipartFile("file", "evidence.png", "image/png", png);
        String uploadResponse = mockMvc.perform(multipart("/api/bugs/{id}/attachments", bugId)
                        .file(evidence)
                        .header("Authorization", bearer(developerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName").value("evidence.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.fileSize").value(png.length))
                .andExpect(jsonPath("$.uploadedByName").value("Dev One"))
                .andReturn().getResponse().getContentAsString();
        long attachmentId = objectMapper.readTree(uploadResponse).path("id").asLong();

        byte[] downloaded = mockMvc.perform(
                        get("/api/bugs/{id}/attachments/{attachmentId}/download", bugId, attachmentId)
                                .header("Authorization", bearer(testerToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("evidence.png")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).isEqualTo(png);

        // Only the uploader (or an administrator) may remove evidence.
        mockMvc.perform(delete("/api/bugs/{id}/attachments/{attachmentId}", bugId, attachmentId)
                        .header("Authorization", bearer(testerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/bugs/{id}/attachments/{attachmentId}", bugId, attachmentId)
                        .header("Authorization", bearer(developerToken)))
                .andExpect(status().isNoContent());

        // Executables are rejected before anything touches the disk.
        MockMultipartFile executable = new MockMultipartFile("file", "payload.exe", "application/octet-stream",
                new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/bugs/{id}/attachments", bugId)
                        .file(executable)
                        .header("Authorization", bearer(developerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Files of type '.exe' are not allowed."));
    }

    @Test
    @DisplayName("search, filters, pagination and dashboard statistics agree with the data")
    void searchFiltersPaginationAndStatistics() throws Exception {
        long unassignedBugId = createBug(testerToken, "Unassigned crash on export", null);
        createBug(testerToken, "Assigned regression in billing", developer.getId());

        mockMvc.perform(get("/api/bugs").header("Authorization", bearer(testerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.sortBy").value("createdAt"))
                .andExpect(jsonPath("$.sortDirection").value("desc"));

        mockMvc.perform(get("/api/bugs").param("unassigned", "true").header("Authorization", bearer(testerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) unassignedBugId))
                .andExpect(jsonPath("$.content[0].title").value("Unassigned crash on export"));

        mockMvc.perform(get("/api/bugs")
                        .param("assigneeId", String.valueOf(developer.getId()))
                        .header("Authorization", bearer(testerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].assignedDeveloperName").value("Dev One"));

        mockMvc.perform(get("/api/bugs").param("search", "billing").header("Authorization", bearer(testerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // size = 1 → two pages; the first/last flags drive the UI pager buttons.
        mockMvc.perform(get("/api/bugs")
                        .param("size", "1")
                        .param("sortBy", "title")
                        .param("sortDirection", "asc")
                        .header("Authorization", bearer(testerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Assigned regression in billing"));

        // An unknown sort field falls back to createdAt instead of failing.
        mockMvc.perform(get("/api/bugs").param("sortBy", "drop table bugs").header("Authorization", bearer(testerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortBy").value("createdAt"));

        mockMvc.perform(get("/api/dashboard/statistics").header("Authorization", bearer(developerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBugs").value(2))
                .andExpect(jsonPath("$.openBugs").value(2))
                .andExpect(jsonPath("$.inProgressBugs").value(0))
                .andExpect(jsonPath("$.unassignedBugs").value(1))
                .andExpect(jsonPath("$.assignedToMe").value(1))
                .andExpect(jsonPath("$.statusDistribution.OPEN").value(2))
                .andExpect(jsonPath("$.severityDistribution.HIGH").value(2))
                .andExpect(jsonPath("$.priorityDistribution.MEDIUM").value(2));
    }

    @Test
    @DisplayName("missing resources, wrong verbs and bad media types map to 404/405/415")
    void mvcErrorsUseTheirOwnStatusCodes() throws Exception {
        // Browsers request this automatically on every page load, so it must never
        // surface as a server error.
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/does-not-exist.html"))
                .andExpect(status().isNotFound());

        // Unknown API paths stay hidden from anonymous callers, and answer 404 once
        // an authenticated caller asks for them.
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/does-not-exist").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());

        // GET on a POST-only endpoint.
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));

        // Body sent in a media type the endpoint does not consume.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("email=admin@integration.test"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

}
