package com.pulsebrief.ingestion.service;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockAiSummaryProvider implements AiSummaryProvider {
    @Override
    public String providerType() {
        return "MOCK";
    }

    @Override
    public AiSummaryProviderResult generate(AiSummaryRequest request) {
        String title = blankToDefault(request.title(), "未命名候选");
        String inputExcerpt = truncate(singleLine(request.inputText()), 140);
        return new AiSummaryProviderResult(
                "Mock AI 摘要：" + title + "。基于已授权输入，" + inputExcerpt,
                List.of(
                        "Mock AI 要点：关注事实是否完整，避免直接发布未审核结论。",
                        "Mock AI 要点：核对来源、时间和授权边界后再进入用户端。",
                        "Mock AI 要点：保留人工编辑空间，不把模型输出视为最终稿。"
                ),
                "Mock AI 影响分析：该草稿仅用于 Admin 审核工作台，发布前仍需人工确认摘要、要点和措辞。",
                "mock-v1",
                0,
                0
        );
    }

    private String singleLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
