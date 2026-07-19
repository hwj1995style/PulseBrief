package com.pulsebrief.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pulsebrief.ingestion.config.DeepSeekSummaryProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pulsebrief.ai.deepseek", name = "enabled", havingValue = "true")
public class DeepSeekSummaryProvider implements AiSummaryProvider {
    private static final String SYSTEM_PROMPT = """
            你是 PulseBrief 的资讯摘要编辑助手。仅依据用户提供的已授权材料生成中文审核草稿。
            不补充材料中不存在的事实、数字或引语；不提供投资建议；不把推测写成事实。
            必须输出 JSON 对象，且只包含 summary、keyPoints、impactAnalysis 三个字段。
            summary 为 180 个汉字以内的字符串；keyPoints 为固定 3 条、每条 60 个汉字以内的字符串数组；
            impactAnalysis 为 140 个汉字以内的字符串，并明确不确定性。
            JSON 示例：{"summary":"摘要","keyPoints":["要点一","要点二","要点三"],"impactAnalysis":"影响及不确定性"}
            """;

    private final DeepSeekSummaryProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AiUsageService usageService;

    @Autowired
    public DeepSeekSummaryProvider(
            DeepSeekSummaryProperties properties,
            ObjectMapper objectMapper,
            AiUsageService usageService
    ) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build(), usageService);
    }

    DeepSeekSummaryProvider(
            DeepSeekSummaryProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            AiUsageService usageService
    ) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "PULSEBRIEF_DEEPSEEK_API_KEY is required when the DeepSeek summary provider is enabled");
        }
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.usageService = usageService;
    }

    @Override
    public String providerType() {
        return "DEEPSEEK";
    }

    @Override
    public String modelName() {
        return properties.model();
    }

    @Override
    public AiSummaryProviderResult generate(AiSummaryRequest request) {
        Long usageEventId = usageService.begin("SUMMARY", providerType(), modelName());
        try {
            String body = objectMapper.writeValueAsString(requestBody(request));
            int totalPromptTokens = 0;
            int totalCompletionTokens = 0;
            for (int attempt = 1; attempt <= 2; attempt++) {
                HttpResponse<String> response = httpClient.send(httpRequest(body), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("DeepSeek Chat API returned HTTP " + response.statusCode());
                }
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode usage = root.path("usage");
                totalPromptTokens += usage.path("prompt_tokens").asInt(0);
                totalCompletionTokens += usage.path("completion_tokens").asInt(0);
                String content = root.path("choices").path(0).path("message").path("content").asText();
                if (!content.isBlank()) {
                    AiSummaryProviderResult parsed = parseResponse(root, content);
                    AiSummaryProviderResult result = new AiSummaryProviderResult(
                            parsed.summary(),
                            parsed.keyPoints(),
                            parsed.impactAnalysis(),
                            parsed.modelName(),
                            totalPromptTokens,
                            totalCompletionTokens
                    );
                    usageService.markSuccess(
                            usageEventId,
                            providerType(),
                            result.tokenPromptCount(),
                            result.tokenCompletionCount()
                    );
                    return result;
                }
                if (attempt == 2) {
                    throw new IllegalStateException("DeepSeek response did not contain output content");
                }
            }
            throw new IllegalStateException("DeepSeek response did not contain output content");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            IllegalStateException failure = new IllegalStateException("DeepSeek summary request was interrupted", exception);
            usageService.markFailed(usageEventId, failure);
            throw failure;
        } catch (Exception exception) {
            RuntimeException failure = exception instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new IllegalStateException("DeepSeek summary request failed: " + exception.getMessage(), exception);
            usageService.markFailed(usageEventId, failure);
            throw failure;
        }
    }

    private HttpRequest httpRequest(String body) {
        return HttpRequest.newBuilder(URI.create(properties.baseUrl()))
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .header("Authorization", "Bearer " + properties.apiKey().trim())
                .header("Content-Type", "application/json")
                .header("X-Client-Request-Id", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private ObjectNode requestBody(AiSummaryRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.model());
        root.put("stream", false);
        root.put("max_tokens", properties.maxOutputTokens());
        root.putObject("thinking").put("type", "disabled");
        root.putObject("response_format").put("type", "json_object");
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
        messages.addObject().put("role", "user").put("content", userInput(request));
        return root;
    }

    private String userInput(AiSummaryRequest request) {
        String input = request.inputText() == null ? "" : request.inputText().trim();
        if (input.length() > properties.maxInputCharacters()) {
            input = input.substring(0, properties.maxInputCharacters());
        }
        return "请根据以下材料输出 JSON 摘要。\n标题：" + safe(request.title())
                + "\n来源：" + safe(request.sourceName())
                + "\n输入类型：" + safe(request.inputSourceType())
                + "\n材料：\n" + input;
    }

    private AiSummaryProviderResult parseResponse(JsonNode root, String content) throws IOException {
        if ("length".equals(root.path("choices").path(0).path("finish_reason").asText())) {
            throw new IllegalStateException("DeepSeek response was truncated by max_tokens");
        }
        JsonNode result = objectMapper.readTree(content);
        String summary = requiredText(result, "summary");
        String impactAnalysis = requiredText(result, "impactAnalysis");
        List<String> keyPoints = new ArrayList<>();
        result.path("keyPoints").forEach(node -> {
            if (node.isTextual() && !node.asText().isBlank()) {
                keyPoints.add(node.asText().trim());
            }
        });
        if (keyPoints.size() != 3) {
            throw new IllegalStateException("DeepSeek response must contain exactly three key points");
        }
        JsonNode usage = root.path("usage");
        return new AiSummaryProviderResult(
                summary,
                List.copyOf(keyPoints),
                impactAnalysis,
                root.path("model").asText(properties.model()),
                usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0)
        );
    }

    private String requiredText(JsonNode node, String field) {
        if (!node.path(field).isTextual()) {
            throw new IllegalStateException("DeepSeek response field must be text: " + field);
        }
        String value = node.path(field).asText().trim();
        if (value.isBlank()) {
            throw new IllegalStateException("DeepSeek response field is empty: " + field);
        }
        return value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
