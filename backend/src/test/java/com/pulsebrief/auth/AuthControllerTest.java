package com.pulsebrief.auth;

import com.pulsebrief.auth.api.AuthController;
import com.pulsebrief.auth.api.AuthResponse;
import com.pulsebrief.auth.api.UserProfileResponse;
import com.pulsebrief.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginRequiresAgreementAccepted() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "wenjin@example.com",
                                  "verificationCode": "123456",
                                  "agreementAccepted": false
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsDevelopmentTokenAndUserProfile() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse(
                "dev-token-1",
                "Bearer",
                604800L,
                new UserProfileResponse(1L, "Wenjin", "", "每天几分钟，掌握全球脉搏")
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "wenjin@example.com",
                                  "verificationCode": "123456",
                                  "agreementAccepted": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").value("dev-token-1"))
                .andExpect(jsonPath("$.data.user.nickname").value("Wenjin"));
    }

    @Test
    void guestReturnsGuestProfileWithoutPersistentToken() throws Exception {
        when(authService.guest()).thenReturn(new AuthResponse(
                "",
                "Guest",
                0L,
                new UserProfileResponse(0L, "游客", "", "游客模式")
        ));

        mockMvc.perform(post("/api/auth/guest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.user.nickname").value("游客"));
    }
}
