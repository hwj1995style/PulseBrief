package com.pulsebrief.admin.api;

import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.ingestion.service.AiUsageService;
import com.pulsebrief.ingestion.service.AiUsageSnapshot;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai-usage")
@Tag(name = "Admin")
public class AdminAiUsageController {
    private final AiUsageService usageService;

    public AdminAiUsageController(AiUsageService usageService) {
        this.usageService = usageService;
    }

    @GetMapping("/today")
    public ApiResponse<AdminAiUsageResponse> today() {
        AiUsageSnapshot snapshot = usageService.todaySnapshot();
        return ApiResponse.ok(new AdminAiUsageResponse(
                snapshot.requestCount(),
                snapshot.successCount(),
                snapshot.failedCount(),
                snapshot.blockedCount(),
                snapshot.promptTokens(),
                snapshot.completionTokens(),
                snapshot.estimatedCostUsd(),
                snapshot.dailyRequestLimit(),
                snapshot.dailyTokenLimit(),
                snapshot.warningPercent(),
                snapshot.alertLevel()
        ));
    }
}
