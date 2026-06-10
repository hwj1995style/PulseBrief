package com.pulsebrief.admin;

import com.pulsebrief.article.service.ArticleService;
import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.service.CandidateArticleGenerationService;
import com.pulsebrief.ingestion.service.RawNewsIngestionService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminCandidateControllerTest {
    private static final String ADMIN_TOKEN = "Bearer dev-admin-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RawNewsIngestionService ingestionService;

    @Autowired
    private CandidateArticleGenerationService candidateGenerationService;

    @Autowired
    private CandidateArticleRepository candidateArticleRepository;

    @Autowired
    private ArticleService articleService;

    @Test
    void requiresAdminTokenForCandidateList() throws Exception {
        mockMvc.perform(get("/api/admin/candidates"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/candidates")
                        .header("Authorization", "Bearer dev-token-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsLocalAdminCorsPreflightWithoutAdminToken() throws Exception {
        mockMvc.perform(options("/api/admin/candidates")
                        .header("Origin", "http://localhost:5188")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5188"));
    }

    @Test
    void returnsPendingCandidateListForAdmin() throws Exception {
        CandidateArticle candidate = createPendingCandidate("admin-list");

        mockMvc.perform(get("/api/admin/candidates")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[?(@.id == " + candidate.getId() + ")].status")
                        .value("PENDING_REVIEW"));
    }

    @Test
    void rejectsPendingCandidateAndKeepsItHiddenFromMobileArticles() throws Exception {
        CandidateArticle candidate = createPendingCandidate("admin-reject");

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/reject")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewNote\":\"来源质量不足\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        CandidateArticle updated = candidateArticleRepository.findById(candidate.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("REJECTED");
        assertThat(updated.getRawNewsItem().getItemStatus()).isEqualTo("REJECTED");
        assertThat(articleService.listArticles("all", 1, 50))
                .noneMatch(article -> article.title().equals(candidate.getTitle()));
    }

    @Test
    void publishesPendingCandidateIntoMobileArticleList() throws Exception {
        CandidateArticle candidate = createPendingCandidate("admin-publish");

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/publish")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aiSummary": "Admin 审核后的 AI 摘要",
                                  "keyPoints": ["要点一", "要点二"],
                                  "impactAnalysis": "发布后进入用户端资讯流。",
                                  "publishNow": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedArticleId").isNumber());

        CandidateArticle updated = candidateArticleRepository.findById(candidate.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("PUBLISHED");
        assertThat(updated.getRawNewsItem().getItemStatus()).isEqualTo("PUBLISHED");
        assertThat(articleService.listArticles("all", 1, 50))
                .anyMatch(article -> article.title().equals(candidate.getTitle()));

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/publish")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishNow\":true}"))
                .andExpect(status().isConflict());
    }

    private CandidateArticle createPendingCandidate(String prefix) {
        String uniquePath = prefix + "-" + UUID.randomUUID();
        String sourceCode = "fixture-" + uniquePath;
        String title = "Admin candidate " + uniquePath;
        ingestionService.ingest(
                sourceCode,
                "MANUAL",
                List.of(new RawNewsPayload(
                        "provider-" + uniquePath,
                        title,
                        "Public market commentary for Admin review.",
                        "Example Markets",
                        "https://example.com/admin/" + uniquePath,
                        null,
                        OffsetDateTime.parse("2026-06-09T09:00:00+08:00"),
                        "en",
                        "US",
                        "{\"id\":\"" + uniquePath + "\"}"
                ))
        );
        candidateGenerationService.generatePendingCandidates(sourceCode, 10);
        return candidateArticleRepository.findByTitle(title).orElseThrow();
    }
}
