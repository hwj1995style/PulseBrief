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
        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody(username, PASSWORD))))
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
}
