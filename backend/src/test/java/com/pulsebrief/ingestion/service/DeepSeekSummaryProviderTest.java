package com.pulsebrief.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsebrief.ingestion.config.DeepSeekSummaryProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeepSeekSummaryProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiUsageService usageService = mock(AiUsageService.class);

    @Test
    void sendsJsonChatRequestAndParsesUsage() throws Exception {
        when(usageService.begin("SUMMARY", "DEEPSEEK", "deepseek-v4-flash")).thenReturn(42L);
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = server((exchange, requestNumber) -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, chatResponse(
                    """
                    {"summary":"测试摘要","keyPoints":["要点一","要点二","要点三"],"impactAnalysis":"影响仍需人工确认"}
                    """,
                    123,
                    45
            ));
        });
        try {
            DeepSeekSummaryProvider provider = provider(server, "test-key");

            AiSummaryProviderResult result = provider.generate(request("已授权的 RSS 摘要"));

            assertThat(authorization.get()).isEqualTo("Bearer test-key");
            JsonNode sent = objectMapper.readTree(requestBody.get());
            assertThat(sent.path("model").asText()).isEqualTo("deepseek-v4-flash");
            assertThat(sent.path("response_format").path("type").asText()).isEqualTo("json_object");
            assertThat(sent.path("thinking").path("type").asText()).isEqualTo("disabled");
            assertThat(sent.path("messages").path(0).path("content").asText()).contains("JSON");
            assertThat(result.summary()).isEqualTo("测试摘要");
            assertThat(result.keyPoints()).containsExactly("要点一", "要点二", "要点三");
            assertThat(result.tokenPromptCount()).isEqualTo(123);
            assertThat(result.tokenCompletionCount()).isEqualTo(45);
            verify(usageService).markSuccess(42L, "DEEPSEEK", 123, 45);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void retriesOneEmptyJsonModeResponse() throws Exception {
        HttpServer server = server((exchange, requestNumber) -> {
            if (requestNumber == 1) {
                respond(exchange, chatResponse("", 0, 0));
            } else {
                respond(exchange, chatResponse(
                        """
                        {"summary":"重试成功","keyPoints":["一","二","三"],"impactAnalysis":"待确认"}
                        """,
                        0,
                        0
                ));
            }
        });
        try {
            DeepSeekSummaryProvider provider = provider(server, "test-key");

            assertThat(provider.generate(request("材料")).summary()).isEqualTo("重试成功");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsEnabledProviderWithoutApiKey() {
        DeepSeekSummaryProperties properties = new DeepSeekSummaryProperties(
                true, " ", "https://api.deepseek.com/chat/completions",
                "deepseek-v4-flash", 10, 500, 600
        );

        assertThatThrownBy(() -> new DeepSeekSummaryProvider(properties, objectMapper, usageService))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PULSEBRIEF_DEEPSEEK_API_KEY");
    }

    private DeepSeekSummaryProvider provider(HttpServer server, String apiKey) {
        DeepSeekSummaryProperties properties = new DeepSeekSummaryProperties(
                true,
                apiKey,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions",
                "deepseek-v4-flash",
                10,
                500,
                600
        );
        return new DeepSeekSummaryProvider(properties, objectMapper, usageService);
    }

    private AiSummaryRequest request(String input) {
        return new AiSummaryRequest(
                "测试标题", "测试来源", LocalDateTime.now(),
                "RSS_SUMMARY", input, "预览", "candidate-summary-v1"
        );
    }

    private String chatResponse(String content, int promptTokens, int completionTokens)
            throws java.io.IOException {
        com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
        root.put("model", "deepseek-v4-flash");
        com.fasterxml.jackson.databind.node.ObjectNode choice = root.putArray("choices").addObject();
        choice.put("finish_reason", "stop");
        choice.putObject("message").put("content", content.trim());
        root.putObject("usage")
                .put("prompt_tokens", promptTokens)
                .put("completion_tokens", completionTokens);
        return objectMapper.writeValueAsString(root);
    }

    private HttpServer server(Handler handler) throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                handler.handle(exchange, requestCount.incrementAndGet());
            } finally {
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
    }

    @FunctionalInterface
    private interface Handler {
        void handle(com.sun.net.httpserver.HttpExchange exchange, int requestNumber) throws java.io.IOException;
    }
}
