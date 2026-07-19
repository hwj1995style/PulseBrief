package com.pulsebrief.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pulsebrief.ingestion.config.DeepSeekClassificationProperties;
import com.pulsebrief.ingestion.config.DeepSeekSummaryProperties;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "pulsebrief.ai.deepseek.classification",
        name = "enabled",
        havingValue = "true"
)
public class DeepSeekClassificationProvider implements CandidateClassificationProvider {
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "ai", "macro", "finance", "technology", "investment_view", "industry", "company", "global"
    );
    private static final String SYSTEM_PROMPT = """
            You classify PulseBrief news metadata into exactly one allowed category.
            Allowed categories: ai, macro, finance, technology, investment_view, industry, company, global.
            Use investment_view only for public research or market views from investment banks and similar institutions.
            Return one JSON object with exactly categoryCode, confidence, and reasonCode.
            confidence must be a number from 0 to 1. reasonCode must be a short UPPER_SNAKE_CASE label.
            Do not add facts, prose, markdown, or additional fields.
            """;

    private final DeepSeekSummaryProperties connectionProperties;
    private final DeepSeekClassificationProperties classificationProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AiUsageService usageService;

    @Autowired
    public DeepSeekClassificationProvider(
            DeepSeekSummaryProperties connectionProperties,
            DeepSeekClassificationProperties classificationProperties,
            ObjectMapper objectMapper,
            AiUsageService usageService
    ) {
        this(connectionProperties, classificationProperties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectionProperties.timeoutSeconds()))
                .build(), usageService);
    }

    DeepSeekClassificationProvider(
            DeepSeekSummaryProperties connectionProperties,
            DeepSeekClassificationProperties classificationProperties,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            AiUsageService usageService
    ) {
        if (connectionProperties.apiKey() == null || connectionProperties.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "PULSEBRIEF_DEEPSEEK_API_KEY is required when DeepSeek classification is enabled");
        }
        this.connectionProperties = connectionProperties;
        this.classificationProperties = classificationProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.usageService = usageService;
    }

    @Override
    public String providerType() {
        return "DEEPSEEK";
    }

    @Override
    public Optional<ClassificationDecision> classify(RawNewsItem rawItem) {
        Long usageEventId = usageService.begin("CLASSIFICATION", providerType(), connectionProperties.model());
        try {
            String body = objectMapper.writeValueAsString(requestBody(rawItem));
            HttpResponse<String> response = httpClient.send(httpRequest(body), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("DeepSeek classification API returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            if ("length".equals(root.path("choices").path(0).path("finish_reason").asText())) {
                throw new IllegalStateException("DeepSeek classification response was truncated");
            }
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content.isBlank()) {
                throw new IllegalStateException("DeepSeek classification response was empty");
            }
            Optional<ClassificationDecision> decision = parseDecision(content);
            JsonNode usage = root.path("usage");
            usageService.markSuccess(
                    usageEventId,
                    providerType(),
                    usage.path("prompt_tokens").asInt(0),
                    usage.path("completion_tokens").asInt(0)
            );
            return decision;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            IllegalStateException failure = new IllegalStateException(
                    "DeepSeek classification request was interrupted", exception);
            usageService.markFailed(usageEventId, failure);
            throw failure;
        } catch (Exception exception) {
            RuntimeException failure = exception instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new IllegalStateException(
                            "DeepSeek classification request failed: " + exception.getMessage(), exception);
            usageService.markFailed(usageEventId, failure);
            throw failure;
        }
    }

    private Optional<ClassificationDecision> parseDecision(String content) throws IOException {
        JsonNode result = objectMapper.readTree(content);
        String categoryCode = result.path("categoryCode").asText().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CATEGORIES.contains(categoryCode)) {
            throw new IllegalStateException("DeepSeek classification returned an unsupported category");
        }
        if (!result.path("confidence").isNumber()) {
            throw new IllegalStateException("DeepSeek classification confidence must be numeric");
        }
        double confidence = result.path("confidence").asDouble();
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalStateException("DeepSeek classification confidence is out of range");
        }
        if (confidence < classificationProperties.minConfidence()) {
            return Optional.empty();
        }
        String reasonCode = sanitizeReason(result.path("reasonCode").asText());
        return Optional.of(new ClassificationDecision(
                categoryCode,
                confidence,
                "MODEL_DEEPSEEK:" + reasonCode
        ));
    }

    private HttpRequest httpRequest(String body) {
        return HttpRequest.newBuilder(URI.create(connectionProperties.baseUrl()))
                .timeout(Duration.ofSeconds(connectionProperties.timeoutSeconds()))
                .header("Authorization", "Bearer " + connectionProperties.apiKey().trim())
                .header("Content-Type", "application/json")
                .header("X-Client-Request-Id", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private ObjectNode requestBody(RawNewsItem rawItem) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", connectionProperties.model());
        root.put("stream", false);
        root.put("max_tokens", classificationProperties.maxOutputTokens());
        root.putObject("thinking").put("type", "disabled");
        root.putObject("response_format").put("type", "json_object");
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
        messages.addObject().put("role", "user").put("content", input(rawItem));
        return root;
    }

    private String input(RawNewsItem rawItem) {
        String input = String.join("\n",
                "Title: " + safe(rawItem.getTitle()),
                "Source: " + safe(rawItem.getSourceName()),
                "RSS summary: " + safe(rawItem.getSummary())
        );
        return input.length() <= classificationProperties.maxInputCharacters()
                ? input
                : input.substring(0, classificationProperties.maxInputCharacters());
    }

    private String sanitizeReason(String value) {
        String normalized = value == null ? "UNSPECIFIED" : value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            normalized = "UNSPECIFIED";
        }
        return normalized.substring(0, Math.min(normalized.length(), 48));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
