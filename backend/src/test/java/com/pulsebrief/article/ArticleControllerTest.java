package com.pulsebrief.article;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.article.api.ArticleController;
import com.pulsebrief.article.api.ArticleDetailResponse;
import com.pulsebrief.article.api.DigestHeroResponse;
import com.pulsebrief.article.api.HomeArticlesResponse;
import com.pulsebrief.article.service.ArticleService;
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

@WebMvcTest(ArticleController.class)
class ArticleControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @Test
    void returnsHomeDigestInvestmentPickAndArticleCards() throws Exception {
        ArticleCardResponse card = new ArticleCardResponse(
                10L,
                "高盛：AI 基建投资仍将持续，电力和算力需求进入新阶段",
                "Goldman Sachs Research",
                "2026-06-08T09:30:00+08:00",
                "investment_view",
                "投行观点",
                "高盛公开观点认为，AI 基础设施投资仍处于扩张阶段。",
                "",
                "02:48",
                true,
                false,
                false
        );
        when(articleService.getHomeArticles("all", 20)).thenReturn(
                new HomeArticlesResponse(
                        new DigestHeroResponse(1L, "今日全球简报", "精选 10 条全球重点资讯"),
                        card,
                        List.of(card)
                )
        );

        mockMvc.perform(get("/api/articles/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.todayDigest.title").value("今日全球简报"))
                .andExpect(jsonPath("$.data.investmentPick.categoryCode").value("investment_view"))
                .andExpect(jsonPath("$.data.articles[0].hot").value(true));
    }

    @Test
    void returnsArticleDetailWithoutFullNewsText() throws Exception {
        when(articleService.getArticleDetail(10L)).thenReturn(new ArticleDetailResponse(
                10L,
                "英伟达推出新一代 AI 芯片 Blackwell Ultra，训练与推理性能再升级",
                "路透社",
                "2026-06-08T09:30:00+08:00",
                "ai",
                "AI 前沿",
                "英伟达正式发布新一代 Blackwell Ultra 平台。",
                List.of("Blackwell Ultra 采用新架构。"),
                "新产品可能推动 AI 基建投资持续加码。",
                "https://example.com/article",
                false,
                List.of()
        ));

        mockMvc.perform(get("/api/articles/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.aiSummary").value("英伟达正式发布新一代 Blackwell Ultra 平台。"))
                .andExpect(jsonPath("$.data.keyPoints[0]").value("Blackwell Ultra 采用新架构。"))
                .andExpect(jsonPath("$.data.originalUrl").value("https://example.com/article"))
                .andExpect(jsonPath("$.data.fullText").doesNotExist());
    }
}
