package com.pulsebrief.playback;

import com.pulsebrief.playback.api.PlaybackController;
import com.pulsebrief.playback.api.PlaybackHistoryResponse;
import com.pulsebrief.playback.service.PlaybackService;
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
}
