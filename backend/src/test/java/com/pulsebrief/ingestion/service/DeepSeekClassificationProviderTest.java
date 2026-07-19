package com.pulsebrief.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsebrief.ingestion.config.DeepSeekClassificationProperties;
import com.pulsebrief.ingestion.config.DeepSeekSummaryProperties;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeepSeekClassificationProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsMetadataOnlyAndParsesAcceptedDecision() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, response("company", 0.82, "company results"));
        });
        try {
            DeepSeekClassificationProvider provider = provider(server, 0.65);

            ClassificationDecision decision = provider.classify(rawItem()).orElseThrow();

            JsonNode sent = objectMapper.readTree(requestBody.get());
            assertThat(sent.path("response_format").path("type").asText()).isEqualTo("json_object");
            assertThat(sent.path("messages").path(1).path("content").asText())
                    .contains("Quarterly business update", "RSS summary")
                    .doesNotContain("full text");
            assertThat(decision.suggestedCategoryCode()).isEqualTo("company");
            assertThat(decision.confidence()).isEqualTo(0.82);
            assertThat(decision.matchedRule()).isEqualTo("MODEL_DEEPSEEK:COMPANY_RESULTS");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsUnsupportedCategory() throws Exception {
        HttpServer server = server(exchange -> respond(exchange, response("sports", 0.91, "sports")));
        try {
            assertThatThrownBy(() -> provider(server, 0.65).classify(rawItem()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("unsupported category");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void returnsEmptyWhenConfidenceIsBelowThreshold() throws Exception {
        HttpServer server = server(exchange -> respond(exchange, response("company", 0.49, "uncertain")));
        try {
            assertThat(provider(server, 0.65).classify(rawItem())).isEmpty();
        } finally {
            server.stop(0);
        }
    }

    private DeepSeekClassificationProvider provider(HttpServer server, double minConfidence) {
        DeepSeekSummaryProperties connection = new DeepSeekSummaryProperties(
                false,
                "test-key",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions",
                "deepseek-v4-flash",
                10,
                500,
                600
        );
        DeepSeekClassificationProperties classification = new DeepSeekClassificationProperties(
                true, minConfidence, 1000, 200
        );
        return new DeepSeekClassificationProvider(
                connection,
                classification,
                objectMapper,
                java.net.http.HttpClient.newHttpClient()
        );
    }

    private RawNewsItem rawItem() {
        return new RawNewsItem(
                "fixture",
                new RawNewsPayload(
                        "id-1",
                        "Quarterly business update",
                        "RSS summary with new operating targets",
                        "Example Source",
                        "https://example.com/update",
                        null,
                        OffsetDateTime.parse("2026-07-19T09:00:00+08:00"),
                        "en",
                        "US",
                        "{}"
                ),
                "url-hash",
                "content-hash"
        );
    }

    private String response(String category, double confidence, String reason) throws Exception {
        String content = objectMapper.writeValueAsString(java.util.Map.of(
                "categoryCode", category,
                "confidence", confidence,
                "reasonCode", reason
        ));
        return objectMapper.writeValueAsString(java.util.Map.of(
                "choices", java.util.List.of(java.util.Map.of(
                        "finish_reason", "stop",
                        "message", java.util.Map.of("content", content)
                ))
        ));
    }

    private HttpServer server(Handler handler) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                handler.handle(exchange);
            } catch (Exception exception) {
                throw new java.io.IOException(exception);
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
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws Exception;
    }
}
