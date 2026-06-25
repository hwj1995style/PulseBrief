package com.pulsebrief.admin;

import com.pulsebrief.article.domain.NewsArticle;
import com.pulsebrief.article.repository.ArticleRepository;
import com.pulsebrief.article.service.ArticleService;
import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.ReportAsset;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.AiSummaryTaskRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.repository.RawNewsContentRepository;
import com.pulsebrief.ingestion.service.CandidateArticleGenerationService;
import com.pulsebrief.ingestion.service.DownloadedPdf;
import com.pulsebrief.ingestion.service.HtmlContentClient;
import com.pulsebrief.ingestion.service.PdfDownloadClient;
import com.pulsebrief.ingestion.service.RawNewsIngestionService;
import com.pulsebrief.ingestion.service.ReportAssetRegistrationService;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "pulsebrief.pdf-cache.storage-dir=target/test-pdf-cache-admin",
        "pulsebrief.pdf-cache.enabled=true"
})
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
    private AiSummaryTaskRepository aiSummaryTaskRepository;

    @Autowired
    private NewsIngestionSourceRepository sourceRepository;

    @Autowired
    private RawNewsContentRepository rawNewsContentRepository;

    @Autowired
    private ReportAssetRegistrationService reportAssetRegistrationService;

    @Autowired
    private ArticleRepository articleRepository;

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
    void updatesPendingCandidateDraftBeforePublish() throws Exception {
        CandidateArticle candidate = createPendingCandidate("admin-update");

        mockMvc.perform(put("/api/admin/candidates/" + candidate.getId())
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "运营修订后的候选标题",
                                  "summary": "运营修订后的候选摘要",
                                  "categoryCode": "ai",
                                  "sourceName": "Updated Source",
                                  "tagNames": ["AI 基建", "算力", "AI 基建"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("运营修订后的候选标题"))
                .andExpect(jsonPath("$.data.summary").value("运营修订后的候选摘要"))
                .andExpect(jsonPath("$.data.categoryCode").value("ai"))
                .andExpect(jsonPath("$.data.sourceName").value("Updated Source"))
                .andExpect(jsonPath("$.data.tagNames[0]").value("AI 基建"))
                .andExpect(jsonPath("$.data.tagNames[1]").value("算力"));

        CandidateArticle updated = candidateArticleRepository.findById(candidate.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("运营修订后的候选标题");
        assertThat(updated.getSummary()).isEqualTo("运营修订后的候选摘要");
        assertThat(updated.getCategoryCode()).isEqualTo("ai");
        assertThat(updated.getSourceName()).isEqualTo("Updated Source");
        assertThat(updated.getTagNames()).isEqualTo("AI 基建,算力");

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/publish")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishNow\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        Long publishedArticleId = candidateArticleRepository.findById(candidate.getId()).orElseThrow().getPublishedArticleId();
        assertThat(publishedArticleId).isNotNull();
        NewsArticle publishedArticle = articleRepository.findById(publishedArticleId).orElseThrow();
        assertThat(publishedArticle.getTitle()).isEqualTo("运营修订后的候选标题");
        assertThat(publishedArticle.getTagNames()).isEqualTo("AI 基建,算力");
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
        Long publishedArticleId = updated.getPublishedArticleId();
        assertThat(publishedArticleId).isNotNull();
        assertThat(articleService.listArticles("all", 1, 50))
                .anyMatch(article -> article.title().equals(candidate.getTitle()));

        mockMvc.perform(get("/api/articles")
                        .param("categoryCode", "all")
                        .param("page", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + publishedArticleId + ")].title")
                        .value(candidate.getTitle()));

        mockMvc.perform(get("/api/articles/" + publishedArticleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(candidate.getTitle()))
                .andExpect(jsonPath("$.data.aiSummary").value("Admin 审核后的 AI 摘要"))
                .andExpect(jsonPath("$.data.keyPoints[0]").value("要点一"))
                .andExpect(jsonPath("$.data.impactAnalysis").value("发布后进入用户端资讯流。"));

        mockMvc.perform(get("/api/admin/operation-logs")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("module", "PUBLISH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.actionType == 'PUBLISH_ARTICLE' && @.targetId == "
                        + publishedArticleId + ")].targetTitle")
                        .value(candidate.getTitle()));

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/publish")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishNow\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void fetchesAuthorizedContentForCandidateAndReturnsItInDetail() throws Exception {
        CandidateArticle candidate = createPendingCandidate("admin-content-fetch", "SNIPPET_ALLOWED");

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/content/fetch")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"SNIPPET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fetchStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.captureMode").value("SNIPPET"))
                .andExpect(jsonPath("$.data.preview").value(org.hamcrest.Matchers.containsString("authorized market context")));

        assertThat(rawNewsContentRepository.findTopByRawNewsItem_IdOrderByFetchedAtDesc(
                candidate.getRawNewsItem().getId()
        )).isPresent();

        mockMvc.perform(get("/api/admin/candidates/" + candidate.getId())
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.fetchStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content.preview").value(org.hamcrest.Matchers.containsString("authorized market context")));
    }

    @Test
    void skipsUnauthorizedContentFetchForCandidate() throws Exception {
        CandidateArticle candidate = createPendingCandidate("admin-content-skip", "SUMMARY_ONLY");

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/content/fetch")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"SNIPPET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fetchStatus").value("SKIPPED"))
                .andExpect(jsonPath("$.data.errorMessage").value(org.hamcrest.Matchers.containsString("not authorized")));

        assertThat(rawNewsContentRepository.findTopByRawNewsItem_IdOrderByFetchedAtDesc(
                candidate.getRawNewsItem().getId()
        )).isEmpty();
    }

    @Test
    void generatesAiSummaryTaskAndReturnsLatestInCandidateDetail() throws Exception {
        CandidateArticle candidate = createPendingCandidate("admin-ai-summary", "SUMMARY_ONLY");

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/ai-summary/generate")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputSourceType": "AUTO",
                                  "providerType": "MOCK",
                                  "promptVersion": "candidate-summary-v1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.inputSourceType").value("RSS_SUMMARY"))
                .andExpect(jsonPath("$.data.providerType").value("MOCK"))
                .andExpect(jsonPath("$.data.modelName").value("mock-v1"))
                .andExpect(jsonPath("$.data.generatedSummary").value(org.hamcrest.Matchers.containsString(candidate.getTitle())))
                .andExpect(jsonPath("$.data.generatedKeyPoints[0]").value(org.hamcrest.Matchers.containsString("Mock AI")));

        mockMvc.perform(get("/api/admin/candidates/" + candidate.getId())
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiSummaryTask.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.aiSummaryTask.providerType").value("MOCK"))
                .andExpect(jsonPath("$.data.aiSummaryTask.generatedSummary")
                        .value(org.hamcrest.Matchers.containsString(candidate.getTitle())));
    }

    @Test
    void appliesSuccessfulAiSummaryTaskForAdminDraft() throws Exception {
        CandidateArticle candidate = createPendingCandidate("admin-ai-apply", "SUMMARY_ONLY");

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/ai-summary/generate")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputSourceType\":\"AUTO\",\"providerType\":\"MOCK\",\"promptVersion\":\"candidate-summary-v1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        Long taskId = aiSummaryTaskRepository
                .findTopByCandidateArticle_IdOrderByCreatedAtDesc(candidate.getId())
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/ai-summary/" + taskId + "/apply")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.generatedSummary").value(org.hamcrest.Matchers.containsString(candidate.getTitle())));
    }

    @Test
    void cachesAndApprovesAuthorizedPdfAssetBeforePublish() throws Exception {
        CandidateArticle candidate = createPendingCandidate("admin-pdf-cache", "PDF_ALLOWED");
        ReportAsset asset = registerPdfAsset(candidate, "admin-pdf-cache");

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId()
                        + "/report-assets/" + asset.getId() + "/cache")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cacheStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.licenseNote").value(org.hamcrest.Matchers.containsString("Fixture source")));

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId()
                        + "/report-assets/" + asset.getId() + "/approve")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewNote\":\"授权公开 PDF 可发布\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.cacheStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.reviewNote").value("授权公开 PDF 可发布"));

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/publish")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishNow\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void rejectsPdfAssetAndAllowsArticlePublishWithoutPdfEntry() throws Exception {
        CandidateArticle candidate = createPendingCandidate("admin-pdf-reject", "PDF_ALLOWED");
        ReportAsset asset = registerPdfAsset(candidate, "admin-pdf-reject");

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId()
                        + "/report-assets/" + asset.getId() + "/reject")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewNote\":\"只保留原文链接\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reviewNote").value("只保留原文链接"));

        mockMvc.perform(post("/api/admin/candidates/" + candidate.getId() + "/publish")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishNow\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    private CandidateArticle createPendingCandidate(String prefix) {
        return createPendingCandidate(prefix, null);
    }

    private CandidateArticle createPendingCandidate(String prefix, String contentAccessPolicy) {
        String uniquePath = prefix + "-" + UUID.randomUUID();
        String sourceCode = "fixture-" + uniquePath;
        String title = "Admin candidate " + uniquePath;
        if (contentAccessPolicy != null) {
            sourceRepository.save(NewsIngestionSource.fixture(
                    sourceCode,
                    "Admin candidate source " + uniquePath,
                    contentAccessPolicy,
                    24
            ));
        }
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
                        contentAccessPolicy == null
                                ? OffsetDateTime.parse("2026-06-09T09:00:00+08:00")
                                : OffsetDateTime.now().minusHours(2),
                        "en",
                        "US",
                        "{\"id\":\"" + uniquePath + "\"}"
                ))
        );
        candidateGenerationService.generatePendingCandidates(sourceCode, 10);
        return candidateArticleRepository.findByTitle(title).orElseThrow();
    }

    private ReportAsset registerPdfAsset(CandidateArticle candidate, String prefix) {
        String uniquePath = prefix + "-" + UUID.randomUUID();
        return reportAssetRegistrationService.registerPdfMetadata(
                candidate.getId(),
                candidate.getRawNewsItem().getSourceCode(),
                "Admin public PDF " + uniquePath,
                "https://example.com/reports/" + uniquePath + ".pdf",
                uniquePath + ".pdf",
                null,
                "legacy-admin-pdf-" + uniquePath,
                "PDF_ALLOWED"
        );
    }

    @TestConfiguration
    static class FixtureContentClientConfig {
        @Bean
        @Primary
        HtmlContentClient fixtureHtmlContentClient() {
            return url -> """
                    <html>
                      <body>
                        <article>
                          <p>Admin authorized market context for candidate review.</p>
                          <p>Admin authorized policy context for compliance review.</p>
                        </article>
                      </body>
                    </html>
                    """;
        }

        @Bean
        @Primary
        PdfDownloadClient fixturePdfDownloadClient() {
            return url -> new DownloadedPdf(
                    "admin-fixture-report.pdf",
                    "application/pdf",
                    "%PDF-1.4\nadmin fixture public report\n%%EOF".getBytes(StandardCharsets.UTF_8)
            );
        }
    }
}
