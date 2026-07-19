package com.pulsebrief.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsebrief.ingestion.config.OpenAiSummaryProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiSummaryProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsStructuredResponsesRequestAndParsesUsage() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/responses", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"model":"gpt-test","output":[{"content":[{"type":"output_text","text":"{\\"summary\\":\\"测试摘要\\",\\"keyPoints\\":[\\"要点一\\",\\"要点二\\",\\"要点三\\"],\\"impactAnalysis\\":\\"影响仍需人工确认\\"}"}]}],"usage":{"input_tokens":123,"output_tokens":45}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            OpenAiSummaryProperties properties = new OpenAiSummaryProperties(
                    true, "test-key", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses",
                    "gpt-test", 10, 500, 600
            );
            OpenAiSummaryProvider provider = new OpenAiSummaryProvider(properties, objectMapper);

            AiSummaryProviderResult result = provider.generate(new AiSummaryRequest(
                    "测试标题", "测试来源", OffsetDateTime.now().toLocalDateTime(),
                    "RSS_SUMMARY", "已授权的 RSS 摘要", "预览", "candidate-summary-v1"
            ));

            assertThat(authorization.get()).isEqualTo("Bearer test-key");
            JsonNode sent = objectMapper.readTree(requestBody.get());
            assertThat(sent.path("model").asText()).isEqualTo("gpt-test");
            assertThat(sent.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
            assertThat(sent.path("text").path("format").path("strict").asBoolean()).isTrue();
            assertThat(result.summary()).isEqualTo("测试摘要");
            assertThat(result.keyPoints()).containsExactly("要点一", "要点二", "要点三");
            assertThat(result.tokenPromptCount()).isEqualTo(123);
            assertThat(result.tokenCompletionCount()).isEqualTo(45);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsEnabledProviderWithoutApiKey() {
        OpenAiSummaryProperties properties = new OpenAiSummaryProperties(
                true, " ", "https://api.openai.com/v1/responses", "gpt-test", 10, 500, 600
        );
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new OpenAiSummaryProvider(properties, objectMapper))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PULSEBRIEF_OPENAI_API_KEY");
    }
}
