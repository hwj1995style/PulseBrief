package com.pulsebrief.admin;

import com.pulsebrief.article.domain.NewsArticle;
import com.pulsebrief.article.repository.ArticleRepository;
import com.pulsebrief.digest.repository.DigestRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDigestControllerTest {
    private static final String ADMIN_TOKEN = "Bearer dev-admin-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private DigestRepository digestRepository;

    @Test
    void requiresAdminTokenForDigestList() throws Exception {
        mockMvc.perform(get("/api/admin/digests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsPublishesDigestAndExposesItToMobileApi() throws Exception {
        NewsArticle article = createPublishedArticle();
        LocalDate digestDate = futureDigestDate();

        mockMvc.perform(get("/api/admin/digests/article-candidates")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("keyword", article.getTitle()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(article.getId()))
                .andExpect(jsonPath("$.data.items[0].title").value(article.getTitle()));

        String createPayload = """
                {
                  "digestDate": "%s",
                  "digestType": "MORNING",
                  "categoryCode": "global",
                  "title": "今日全球早报：Admin 发布链路验证",
                  "summary": "精选已发布文章生成今日简报",
                  "audioText": "欢迎收听脉闻今日全球早报，第一条关注 AI 基建投资。",
                  "articles": [
                    {
                      "articleId": %d,
                      "sortNo": 1,
                      "highlightText": "AI 基建投资继续扩张"
                    }
                  ]
                }
                """.formatted(digestDate, article.getId());

        String createdContent = mockMvc.perform(post("/api/admin/digests")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.content").value(containsString("AI 基建投资继续扩张")))
                .andExpect(jsonPath("$.data.articles[0].articleId").value(article.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long digestId = extractId(createdContent);

        mockMvc.perform(post("/api/admin/digests/" + digestId + "/publish")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishNow\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/admin/operation-logs")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("module", "PUBLISH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.actionType == 'PUBLISH_DIGEST' && @.targetId == "
                        + digestId + ")].targetTitle")
                        .value("今日全球早报：Admin 发布链路验证"));

        mockMvc.perform(get("/api/digests/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value(digestDate.toString()))
                .andExpect(jsonPath("$.data.headline.title").value("今日全球早报：Admin 发布链路验证"))
                .andExpect(jsonPath("$.data.highlights[0]").value("AI 基建投资继续扩张"));

        mockMvc.perform(get("/api/digests/" + digestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audioText").value("欢迎收听脉闻今日全球早报，第一条关注 AI 基建投资。"))
                .andExpect(jsonPath("$.data.points[0]").value("AI 基建投资继续扩张"));

        mockMvc.perform(post("/api/admin/digests/" + digestId + "/publish")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishNow\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updatesDraftDigestAndReplacesArticleHighlights() throws Exception {
        NewsArticle firstArticle = createPublishedArticle();
        NewsArticle secondArticle = createPublishedArticle();
        LocalDate digestDate = futureDigestDate();
        Long digestId = createDraftDigest(digestDate, firstArticle.getId());

        String updatePayload = """
                {
                  "digestDate": "%s",
                  "digestType": "MORNING",
                  "categoryCode": "finance",
                  "title": "午前市场简报：更新后标题",
                  "summary": "运营手动更新后的摘要",
                  "audioText": "这是更新后的播报文案。",
                  "articles": [
                    {
                      "articleId": %d,
                      "sortNo": 1,
                      "highlightText": "第二条文章进入简报"
                    }
                  ]
                }
                """.formatted(digestDate, secondArticle.getId());

        mockMvc.perform(put("/api/admin/digests/" + digestId)
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.categoryCode").value("finance"))
                .andExpect(jsonPath("$.data.title").value("午前市场简报：更新后标题"))
                .andExpect(jsonPath("$.data.audioText").value("这是更新后的播报文案。"))
                .andExpect(jsonPath("$.data.content").value("第二条文章进入简报"))
                .andExpect(jsonPath("$.data.articles.length()").value(1))
                .andExpect(jsonPath("$.data.articles[0].articleId").value(secondArticle.getId()))
                .andExpect(jsonPath("$.data.articles[0].highlightText").value("第二条文章进入简报"));

        mockMvc.perform(post("/api/admin/digests/" + digestId + "/publish")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishNow\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(put("/api/admin/digests/" + digestId)
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isConflict());
    }

    @Test
    void offlinesPublishedDigestAndHidesItFromMobileDetail() throws Exception {
        NewsArticle article = createPublishedArticle();
        LocalDate digestDate = futureDigestDate();
        Long digestId = createDraftDigest(digestDate, article.getId());

        mockMvc.perform(post("/api/admin/digests/" + digestId + "/publish")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishNow\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/admin/digests/" + digestId + "/offline")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFFLINE"))
                .andExpect(jsonPath("$.data.availableActions.length()").value(0));

        mockMvc.perform(get("/api/admin/operation-logs")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("module", "PUBLISH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.actionType == 'OFFLINE_DIGEST' && @.targetId == "
                        + digestId + ")].targetTitle")
                        .value("今日全球早报：草稿"));

        mockMvc.perform(get("/api/digests/" + digestId))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/admin/digests/" + digestId + "/offline")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isConflict());
    }

    private NewsArticle createPublishedArticle() {
        String unique = UUID.randomUUID().toString();
        return articleRepository.save(new NewsArticle(
                "Admin digest source article " + unique,
                "AI infrastructure investment remains resilient.",
                "Admin 审核后的 AI 摘要",
                "AI 基建投资继续扩张\n数据中心需求保持强劲",
                "发布后可进入每日简报。",
                "Example Markets",
                "https://example.com/admin-digest/" + unique,
                "ai",
                LocalDateTime.now().minusHours(1),
                "admin-digest-" + unique
        ));
    }

    private Long createDraftDigest(LocalDate digestDate, Long articleId) throws Exception {
        String createPayload = """
                {
                  "digestDate": "%s",
                  "digestType": "MORNING",
                  "categoryCode": "global",
                  "title": "今日全球早报：草稿",
                  "summary": "创建后等待编辑",
                  "audioText": "初始播报文案。",
                  "articles": [
                    {
                      "articleId": %d,
                      "sortNo": 1,
                      "highlightText": "初始热点"
                    }
                  ]
                }
                """.formatted(digestDate, articleId);
        String content = mockMvc.perform(post("/api/admin/digests")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(content);
    }

    private LocalDate futureDigestDate() {
        return digestRepository.findAll()
                .stream()
                .map(digest -> digest.getDigestDate())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now())
                .plusDays(1);
    }

    private Long extractId(String content) {
        String marker = "\"id\":";
        int start = content.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("id not found in response: " + content);
        }
        int valueStart = start + marker.length();
        int valueEnd = valueStart;
        while (valueEnd < content.length() && Character.isDigit(content.charAt(valueEnd))) {
            valueEnd++;
        }
        return Long.parseLong(content.substring(valueStart, valueEnd));
    }
}
