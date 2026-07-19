package com.pulsebrief.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pulsebrief.ingestion.config.OpenAiSummaryProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pulsebrief.ai.openai", name = "enabled", havingValue = "true")
public class OpenAiSummaryProvider implements AiSummaryProvider {
    private static final String INSTRUCTIONS = """
            你是 PulseBrief 的资讯摘要编辑助手。仅依据用户提供的已授权材料生成中文审核草稿。
            不补充材料中不存在的事实、数字或引语；不提供投资建议；不把推测写成事实。
            summary 控制在 180 个汉字以内，keyPoints 固定 3 条，每条 60 个汉字以内，
            impactAnalysis 控制在 140 个汉字以内，并明确不确定性。输出必须符合给定 JSON Schema。
            """;

    private final OpenAiSummaryProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiSummaryProvider(OpenAiSummaryProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build());
    }

    OpenAiSummaryProvider(OpenAiSummaryProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "PULSEBRIEF_OPENAI_API_KEY is required when the OpenAI summary provider is enabled");
        }
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String providerType() {
        return "OPENAI";
    }

    @Override
    public String modelName() {
        return properties.model();
    }

    @Override
    public AiSummaryProviderResult generate(AiSummaryRequest request) {
        try {
            String body = objectMapper.writeValueAsString(requestBody(request));
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(properties.baseUrl()))
                    .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.apiKey().trim())
                    .header("Content-Type", "application/json")
                    .header("X-Client-Request-Id", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI Responses API returned HTTP " + response.statusCode());
            }
            return parseResponse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI summary request was interrupted", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("OpenAI summary request failed: " + exception.getMessage(), exception);
        }
    }

    private ObjectNode requestBody(AiSummaryRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.model());
        root.put("instructions", INSTRUCTIONS);
        root.put("input", userInput(request));
        root.put("max_output_tokens", properties.maxOutputTokens());
        ObjectNode format = root.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", "pulsebrief_summary");
        format.put("strict", true);
        format.set("schema", responseSchema());
        return root;
    }

    private String userInput(AiSummaryRequest request) {
        String input = request.inputText() == null ? "" : request.inputText().trim();
        if (input.length() > properties.maxInputCharacters()) {
            input = input.substring(0, properties.maxInputCharacters());
        }
        return "标题：" + safe(request.title()) + "\n来源：" + safe(request.sourceName())
                + "\n输入类型：" + safe(request.inputSourceType()) + "\n材料：\n" + input;
    }

    private ObjectNode responseSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.putArray("required").add("summary").add("keyPoints").add("impactAnalysis");
        schema.put("additionalProperties", false);
        ObjectNode propertiesNode = schema.putObject("properties");
        propertiesNode.putObject("summary").put("type", "string");
        ObjectNode keyPoints = propertiesNode.putObject("keyPoints");
        keyPoints.put("type", "array");
        keyPoints.put("minItems", 3);
        keyPoints.put("maxItems", 3);
        keyPoints.putObject("items").put("type", "string");
        propertiesNode.putObject("impactAnalysis").put("type", "string");
        return schema;
    }

    private AiSummaryProviderResult parseResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        String outputText = findOutputText(root);
        if (outputText == null || outputText.isBlank()) {
            throw new IllegalStateException("OpenAI response did not contain output text");
        }
        JsonNode result = objectMapper.readTree(outputText);
        String summary = requiredText(result, "summary");
        String impactAnalysis = requiredText(result, "impactAnalysis");
        List<String> keyPoints = new ArrayList<>();
        result.path("keyPoints").forEach(node -> {
            if (!node.asText().isBlank()) {
                keyPoints.add(node.asText().trim());
            }
        });
        if (keyPoints.size() != 3) {
            throw new IllegalStateException("OpenAI response must contain exactly three key points");
        }
        JsonNode usage = root.path("usage");
        return new AiSummaryProviderResult(
                summary,
                List.copyOf(keyPoints),
                impactAnalysis,
                root.path("model").asText(properties.model()),
                usage.path("input_tokens").asInt(0),
                usage.path("output_tokens").asInt(0)
        );
    }

    private String findOutputText(JsonNode root) {
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText();
                }
            }
        }
        return null;
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText().trim();
        if (value.isBlank()) {
            throw new IllegalStateException("OpenAI response field is empty: " + field);
        }
        return value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
