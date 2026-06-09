package com.pulsebrief.readhistory;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.readhistory.api.ReadHistoryController;
import com.pulsebrief.readhistory.api.ReadHistoryRecordResponse;
import com.pulsebrief.readhistory.service.ReadHistoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReadHistoryController.class)
class ReadHistoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReadHistoryService readHistoryService;

    @Test
    void recordsReadHistoryForLoggedInUser() throws Exception {
        when(readHistoryService.recordReadHistory(any(), any())).thenReturn(new ReadHistoryRecordResponse(77L));

        mockMvc.perform(post("/api/user/read-history")
                        .header("Authorization", "Bearer dev-token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "articleId": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value(77));
    }

    @Test
    void listsReadHistoryForLoggedInUser() throws Exception {
        when(readHistoryService.listReadHistory(1L, 1, 20)).thenReturn(List.of(new ArticleCardResponse(
                10L,
                "高盛：AI 基建投资仍将持续",
                "Goldman Sachs Research",
                "2026-06-09T09:30:00+08:00",
                "investment_view",
                "投行观点",
                "AI 基础设施投资仍处于扩张阶段。",
                "",
                "02:48",
                true,
                false,
                false
        )));

        mockMvc.perform(get("/api/user/read-history")
                        .header("Authorization", "Bearer dev-token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(10));
    }
}
