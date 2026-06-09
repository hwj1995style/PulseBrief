package com.pulsebrief.playback;

import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.playback.api.PlaybackController;
import com.pulsebrief.playback.api.PlaybackHistoryItemResponse;
import com.pulsebrief.playback.api.PlaybackHistoryResponse;
import com.pulsebrief.playback.service.PlaybackService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaybackController.class)
class PlaybackControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaybackService playbackService;

    @Test
    void recordsPlaybackHistoryForLoggedInUser() throws Exception {
        when(playbackService.recordPlayback(any(), any())).thenReturn(new PlaybackHistoryResponse(99L));

        mockMvc.perform(post("/api/playback/history")
                        .header("Authorization", "Bearer dev-token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playType": "DIGEST",
                                  "articleId": null,
                                  "digestId": 1,
                                  "playTitle": "今日全球早报",
                                  "durationSeconds": 492
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value(99));
    }

    @Test
    void listsPlaybackHistoryForLoggedInUser() throws Exception {
        when(playbackService.listPlaybackHistory(1L, 1, 20)).thenReturn(PageResponse.of(List.of(new PlaybackHistoryItemResponse(
                99L,
                "ARTICLE",
                10L,
                null,
                "高盛：AI 基建投资仍将持续",
                "2026-06-09T09:30:00+08:00",
                168
        )), 1, 20, 1L));

        mockMvc.perform(get("/api/playback/history")
                        .header("Authorization", "Bearer dev-token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].id").value(99))
                .andExpect(jsonPath("$.data.items[0].playType").value("ARTICLE"))
                .andExpect(jsonPath("$.data.items[0].durationSeconds").value(168))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.hasMore").value(false));
    }

    @Test
    void clearsPlaybackHistoryForLoggedInUser() throws Exception {
        when(playbackService.clearPlaybackHistory(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/playback/history")
                        .header("Authorization", "Bearer dev-token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.cleared").value(true));
    }
}
