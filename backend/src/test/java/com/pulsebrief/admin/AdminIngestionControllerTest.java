package com.pulsebrief.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsebrief.ingestion.domain.NewsIngestionJob;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.repository.NewsIngestionJobRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminIngestionControllerTest {
    private static final String ADMIN_TOKEN = "Bearer dev-admin-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NewsIngestionJobRepository jobRepository;

    @Autowired
    private NewsIngestionSourceRepository sourceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void requiresAdminTokenForIngestionJobs() throws Exception {
        mockMvc.perform(get("/api/admin/ingestion/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsIngestionJobsWithFailureLogsAndTodayMetrics() throws Exception {
        String sourceCode = "ops-" + UUID.randomUUID();
        sourceRepository.save(NewsIngestionSource.fixture(
                sourceCode,
                "Ops Monitor Fixture",
                "SUMMARY_ONLY",
                24
        ));
        NewsIngestionJob success = new NewsIngestionJob(sourceCode, "MANUAL");
        success.complete(3, 2, 1, 0);
        jobRepository.save(success);
        NewsIngestionJob failed = new NewsIngestionJob(sourceCode, "SCHEDULED");
        failed.fail("Provider timeout");
        jobRepository.save(failed);

        mockMvc.perform(get("/api/admin/ingestion/jobs")
                        .param("status", "FAILED")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.sourceCode == '" + sourceCode + "')].status")
                        .value("FAILED"))
                .andExpect(jsonPath("$.data.items[?(@.sourceCode == '" + sourceCode + "')].errorMessage")
                        .value("Provider timeout"));

        String metricsJson = mockMvc.perform(get("/api/admin/ingestion/metrics/today")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fetchedCount").isNumber())
                .andExpect(jsonPath("$.data.candidateCount").isNumber())
                .andExpect(jsonPath("$.data.publishedCount").isNumber())
                .andExpect(jsonPath("$.data.failedCount").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode metrics = objectMapper.readTree(metricsJson).path("data");
        assertThat(metrics.path("fetchedCount").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(metrics.path("failedCount").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void returnsIngestionSourcesForAdmin() throws Exception {
        String sourceCode = "source-" + UUID.randomUUID();
        sourceRepository.save(NewsIngestionSource.fixture(
                sourceCode,
                "Source Monitor Fixture",
                "PDF_ALLOWED",
                72
        ));

        mockMvc.perform(get("/api/admin/ingestion/sources")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == '" + sourceCode + "')].name")
                        .value("Source Monitor Fixture"))
                .andExpect(jsonPath("$.data[?(@.code == '" + sourceCode + "')].enabled")
                        .value(true))
                .andExpect(jsonPath("$.data[?(@.code == '" + sourceCode + "')].contentAccessPolicy")
                        .value("PDF_ALLOWED"))
                .andExpect(jsonPath("$.data[?(@.code == '" + sourceCode + "')].maxAgeHours")
                        .value(72))
                .andExpect(jsonPath("$.data[?(@.code == '" + sourceCode + "')].allowPdfDownload")
                        .value(true));
    }

    @Test
    void updatesIngestionSourceEnabledState() throws Exception {
        String sourceCode = "toggle-" + UUID.randomUUID();
        NewsIngestionSource source = sourceRepository.save(NewsIngestionSource.fixture(
                sourceCode,
                "Toggle Fixture",
                "SUMMARY_ONLY",
                24
        ));

        mockMvc.perform(put("/api/admin/ingestion/sources/" + source.getId() + "/enabled")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType("application/json")
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(source.getId()))
                .andExpect(jsonPath("$.data.code").value(sourceCode))
                .andExpect(jsonPath("$.data.enabled").value(false));

        assertThat(sourceRepository.findByCode(sourceCode).orElseThrow().isEnabled()).isFalse();

        mockMvc.perform(put("/api/admin/ingestion/sources/" + source.getId() + "/enabled")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType("application/json")
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        assertThat(sourceRepository.findByCode(sourceCode).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void returnsRawNewsQualityAnomaliesForAdmin() throws Exception {
        String suffix = UUID.randomUUID().toString();
        LocalDateTime databaseNow = databaseNow();
        insertRawNewsItem(
                "quality-" + suffix,
                "缺来源样本 " + suffix,
                "   ",
                "https://example.com/missing-source-" + suffix,
                databaseNow.minusHours(1),
                "missing-source-" + suffix
        );
        insertRawNewsItem(
                "quality-" + suffix,
                "缺链接样本 " + suffix,
                "Quality Source",
                "   ",
                databaseNow.minusHours(1),
                "missing-url-" + suffix
        );
        insertRawNewsItem(
                "quality-" + suffix,
                "缺发布时间样本 " + suffix,
                "Quality Source",
                "https://example.com/missing-published-at-" + suffix,
                null,
                "missing-published-at-" + suffix
        );
        insertRawNewsItem(
                "quality-" + suffix,
                "未来发布时间样本 " + suffix,
                "Quality Source",
                "https://example.com/future-published-at-" + suffix,
                databaseNow.plusHours(2),
                "future-published-at-" + suffix
        );

        mockMvc.perform(get("/api/admin/ingestion/anomalies")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.title == '缺来源样本 " + suffix + "')].issueType")
                        .value("MISSING_SOURCE"))
                .andExpect(jsonPath("$.data.items[?(@.title == '缺链接样本 " + suffix + "')].issueType")
                        .value("MISSING_ORIGINAL_URL"))
                .andExpect(jsonPath("$.data.items[?(@.title == '缺发布时间样本 " + suffix + "')].issueType")
                        .value("PUBLISHED_AT_MISSING"))
                .andExpect(jsonPath("$.data.items[?(@.title == '未来发布时间样本 " + suffix + "')].issueType")
                        .value("PUBLISHED_AT_IN_FUTURE"))
                .andExpect(jsonPath("$.data.items[?(@.title == '缺链接样本 " + suffix + "')].severity")
                        .value("HIGH"));
    }

    private void insertRawNewsItem(
            String sourceCode,
            String title,
            String sourceName,
            String originalUrl,
            LocalDateTime publishedAt,
            String uniqueKey
    ) {
        LocalDateTime now = databaseNow();
        jdbcTemplate.update("""
                insert into raw_news_item (
                    source_code,
                    provider_item_id,
                    title,
                    summary,
                    source_name,
                    original_url,
                    original_url_hash,
                    image_url,
                    published_at,
                    fetched_at,
                    language,
                    country,
                    raw_payload,
                    content_hash,
                    item_status,
                    duplicate_of_id,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as json), ?, ?, ?, ?, ?)
                """,
                sourceCode,
                uniqueKey,
                title,
                "异常检测测试摘要",
                sourceName,
                originalUrl,
                "hash-" + uniqueKey,
                null,
                publishedAt,
                now,
                "zh",
                "CN",
                "{}",
                "content-" + uniqueKey,
                "NEW",
                null,
                now,
                now
        );
    }

    private LocalDateTime databaseNow() {
        String databaseNow = jdbcTemplate.queryForObject(
                "select date_format(now(), '%Y-%m-%dT%H:%i:%s')",
                String.class
        );
        return LocalDateTime.parse(databaseNow);
    }
}
