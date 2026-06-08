package com.pulsebrief.digest;

import com.pulsebrief.digest.api.DigestController;
import com.pulsebrief.digest.api.DigestDetailResponse;
import com.pulsebrief.digest.api.DigestSummaryResponse;
import com.pulsebrief.digest.api.TodayDigestResponse;
import com.pulsebrief.digest.service.DigestService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DigestController.class)
class DigestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DigestService digestService;

    @Test
    void returnsTodayDigestForBriefPage() throws Exception {
        DigestSummaryResponse morning = new DigestSummaryResponse(
                1L,
                "今日全球早报",
                "精选全球、财经、AI 与投行观点 10 条重点",
                "08:12",
                "08:30 更新"
        );
        when(digestService.getTodayDigest()).thenReturn(new TodayDigestResponse(
                "2026-06-08",
                morning,
                List.of(morning),
                List.of("英伟达 Blackwell Ultra 发布", "美联储维持利率不变")
        ));

        mockMvc.perform(get("/api/digests/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.headline.title").value("今日全球早报"))
                .andExpect(jsonPath("$.data.highlights[0]").value("英伟达 Blackwell Ultra 发布"));
    }

    @Test
    void returnsDigestDetailForPlayerPage() throws Exception {
        when(digestService.getDigestDetail(1L)).thenReturn(new DigestDetailResponse(
                1L,
                "今日全球早报：美股反弹，AI 与市场热点快速梳理",
                "脉闻语音简报",
                "MORNING",
                "08:12",
                "2026-06-08T08:30:00+08:00",
                "精选 10 条重点资讯",
                "今天的全球早报包括美股、AI、宏观政策等重点。",
                List.of("英伟达 Blackwell Ultra 将于下半年量产。")
        ));

        mockMvc.perform(get("/api/digests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sourceName").value("脉闻语音简报"))
                .andExpect(jsonPath("$.data.audioText").value("今天的全球早报包括美股、AI、宏观政策等重点。"));
    }
}
