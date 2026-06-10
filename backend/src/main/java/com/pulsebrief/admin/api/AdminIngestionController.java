package com.pulsebrief.admin.api;

import com.pulsebrief.admin.service.AdminIngestionApplicationService;
import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.common.api.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ingestion")
@Tag(name = "Admin")
public class AdminIngestionController {
    private final AdminIngestionApplicationService ingestionService;

    public AdminIngestionController(AdminIngestionApplicationService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @GetMapping("/jobs")
    public ApiResponse<PageResponse<AdminIngestionJobResponse>> listJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(ingestionService.listJobs(status, page, pageSize));
    }

    @GetMapping("/metrics/today")
    public ApiResponse<AdminIngestionMetricsResponse> todayMetrics() {
        return ApiResponse.ok(ingestionService.todayMetrics());
    }

    @GetMapping("/anomalies")
    public ApiResponse<PageResponse<AdminIngestionAnomalyResponse>> listAnomalies(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(ingestionService.listAnomalies(page, pageSize));
    }

    @GetMapping("/sources")
    public ApiResponse<List<AdminIngestionSourceResponse>> listSources() {
        return ApiResponse.ok(ingestionService.listSources());
    }

    @PutMapping("/sources/{id}/enabled")
    public ApiResponse<AdminIngestionSourceResponse> updateSourceEnabled(
            @PathVariable Long id,
            @RequestBody AdminIngestionSourceEnabledRequest request
    ) {
        return ApiResponse.ok(ingestionService.updateSourceEnabled(id, request == null ? null : request.enabled()));
    }
}
