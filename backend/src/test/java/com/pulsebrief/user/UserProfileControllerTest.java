package com.pulsebrief.user;

import com.pulsebrief.user.api.UserProfileController;
import com.pulsebrief.user.api.UserProfileSummaryResponse;
import com.pulsebrief.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    void rejectsProfileWithoutToken() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsProfileForLoggedInUser() throws Exception {
        when(userProfileService.getProfile(1L)).thenReturn(new UserProfileSummaryResponse(
                1L,
                "Wenjin",
                "",
                "每天几分钟，掌握全球脉搏",
                4,
                2,
                0,
                3
        ));

        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer dev-token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.nickname").value("Wenjin"))
                .andExpect(jsonPath("$.data.subscriptionCount").value(4))
                .andExpect(jsonPath("$.data.favoriteCount").value(2))
                .andExpect(jsonPath("$.data.readCount").value(0))
                .andExpect(jsonPath("$.data.playCount").value(3));
    }
}
