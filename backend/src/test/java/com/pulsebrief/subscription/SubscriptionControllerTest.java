package com.pulsebrief.subscription;

import com.pulsebrief.subscription.api.PushPreferencesResponse;
import com.pulsebrief.subscription.api.SubscriptionController;
import com.pulsebrief.subscription.api.SubscriptionResponse;
import com.pulsebrief.subscription.service.SubscriptionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    void rejectsSubscriptionsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/user/subscriptions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsCurrentSubscriptionsForLoggedInUser() throws Exception {
        when(subscriptionService.getSubscriptions(1L)).thenReturn(new SubscriptionResponse(
                List.of("global", "finance", "ai"),
                42,
                new PushPreferencesResponse(true, true, false, false)
        ));

        mockMvc.perform(get("/api/user/subscriptions")
                        .header("Authorization", "Bearer dev-token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.selectedCategoryCodes[0]").value("global"))
                .andExpect(jsonPath("$.data.todayMatchedCount").value(42));
    }

    @Test
    void savesSubscriptionsForLoggedInUser() throws Exception {
        when(subscriptionService.saveSubscriptions(any(), any())).thenReturn(new SubscriptionResponse(
                List.of("global", "finance"),
                42,
                new PushPreferencesResponse(true, true, false, false)
        ));

        mockMvc.perform(put("/api/user/subscriptions")
                        .header("Authorization", "Bearer dev-token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryCodes": ["global", "finance"],
                                  "pushPreferences": {
                                    "morningDigest": true,
                                    "eveningReview": true,
                                    "breakingNews": false,
                                    "investmentView": false
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.selectedCategoryCodes[1]").value("finance"));
    }
}
