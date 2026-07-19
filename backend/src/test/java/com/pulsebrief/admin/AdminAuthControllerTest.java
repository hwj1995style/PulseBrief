package com.pulsebrief.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsebrief.admin.domain.AdminUser;
import com.pulsebrief.admin.repository.AdminOperationLogRepository;
import com.pulsebrief.admin.repository.AdminSessionRepository;
import com.pulsebrief.admin.repository.AdminUserRepository;
import com.pulsebrief.admin.security.AdminIdentityService;
import com.pulsebrief.admin.security.AdminPrincipal;
import com.pulsebrief.admin.service.AdminOperationLogService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAuthControllerTest {
    private static final String PASSWORD = "Strong-Test-Password-2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminUserRepository userRepository;

    @Autowired
    private AdminSessionRepository sessionRepository;

    @Autowired
    private AdminOperationLogRepository operationLogRepository;

    @Autowired
    private AdminOperationLogService operationLogService;

    @Test
    void logsInRestoresIdentityAndRevokesSession() throws Exception {
        AdminUser user = createUser("EDITOR");

        String token = login(user.getUsername());

        mockMvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(user.getUsername()))
                .andExpect(jsonPath("$.data.role").value("EDITOR"));

        mockMvc.perform(post("/api/admin/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enforcesViewerEditorAndAdminWriteBoundaries() throws Exception {
        String viewerToken = login(createUser("VIEWER").getUsername());
        String editorToken = login(createUser("EDITOR").getUsername());

        mockMvc.perform(get("/api/admin/candidates").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/candidates/999999/reject")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/candidates/999999/reject")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/admin/ingestion/sources/999999/enabled")
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsWrongPasswordWithoutCreatingSession() throws Exception {
        AdminUser user = createUser("ADMIN");
        long before = sessionRepository.count();

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody(user.getUsername(), "wrong-password"))))
                .andExpect(status().isUnauthorized());

        assertThat(sessionRepository.count()).isEqualTo(before);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getFailedLoginCount()).isEqualTo(1);
    }

    @Test
    void createsAdminAccountAndForcesTemporaryPasswordRotation() throws Exception {
        String adminToken = login(createUser("ADMIN").getUsername());
        String username = "new-editor-" + UUID.randomUUID().toString().substring(0, 8);
        String temporaryPassword = "Temporary-Strong-2026!";
        String newPassword = "Rotated-Strong-2026!";

        String createdResponse = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserBody(
                                username, "New Editor", "EDITOR", temporaryPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();
        long createdUserId = objectMapper.readTree(createdResponse).path("data").path("id").asLong();

        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody(username, temporaryPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();
        String temporaryToken = objectMapper.readTree(response).path("data").path("token").asText();

        mockMvc.perform(get("/api/admin/candidates").header("Authorization", "Bearer " + temporaryToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/auth/password")
                        .header("Authorization", "Bearer " + temporaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordBody(temporaryPassword, newPassword))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + temporaryToken))
                .andExpect(status().isUnauthorized());

        String rotatedToken = loginWithPassword(username, newPassword);
        mockMvc.perform(get("/api/admin/candidates").header("Authorization", "Bearer " + rotatedToken))
                .andExpect(status().isOk());

        String resetPassword = "Reset-Strong-2026!";
        mockMvc.perform(post("/api/admin/users/{id}/password-reset", createdUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordBody(resetPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(true));
        mockMvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + rotatedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restrictsAccountManagementToAdminAndAddsSecurityHeaders() throws Exception {
        String viewerToken = login(createUser("VIEWER").getUsername());

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Content-Security-Policy"))
                        .contains("default-src 'none'"));
    }

    @Test
    @Transactional
    void recordsAuthenticatedIdentityInOperationLog() {
        AdminUser user = createUser("EDITOR");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                AdminIdentityService.PRINCIPAL_ATTRIBUTE,
                new AdminPrincipal(user.getId(), user.getUsername(), user.getDisplayName(), user.getRoleCode())
        );
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            String title = "Identity audit " + UUID.randomUUID();
            operationLogService.recordArticlePublish(999999L, title);

            var log = operationLogRepository.findAll().stream()
                    .filter(item -> title.equals(item.getTargetTitle()))
                    .findFirst()
                    .orElseThrow();
            assertThat(log.getOperatorUserId()).isEqualTo(user.getId());
            assertThat(log.getOperatorName()).isEqualTo(user.getUsername());
            assertThat(log.getOperatorRole()).isEqualTo("EDITOR");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private AdminUser createUser(String role) {
        String username = role.toLowerCase() + "-" + UUID.randomUUID();
        return userRepository.save(new AdminUser(
                username,
                new BCryptPasswordEncoder(12).encode(PASSWORD),
                role + " Test",
                role
        ));
    }

    private String login(String username) throws Exception {
        return loginWithPassword(username, PASSWORD);
    }

    private String loginWithPassword(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody(username, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.path("data").path("token").asText();
    }

    private record LoginBody(String username, String password) {
    }

    private record CreateUserBody(String username, String displayName, String role, String temporaryPassword) {
    }

    private record PasswordBody(String currentPassword, String newPassword) {
    }

    private record ResetPasswordBody(String temporaryPassword) {
    }
}
