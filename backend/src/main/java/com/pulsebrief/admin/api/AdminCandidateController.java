package com.pulsebrief.admin.api;

import com.pulsebrief.admin.service.AdminCandidateApplicationService;
import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.common.api.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/candidates")
@Tag(name = "Admin")
public class AdminCandidateController {
    private final AdminCandidateApplicationService candidateService;

    public AdminCandidateController(AdminCandidateApplicationService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminCandidateResponse>> listCandidates(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(candidateService.listCandidates(status, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminCandidateDetailResponse> getCandidate(@PathVariable Long id) {
        return ApiResponse.ok(candidateService.getCandidate(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCandidateResponse> updateCandidate(
            @PathVariable Long id,
            @RequestBody AdminCandidateUpdateRequest request
    ) {
        return ApiResponse.ok(candidateService.updateCandidate(id, request));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<AdminCandidateResponse> rejectCandidate(
            @PathVariable Long id,
            @RequestBody(required = false) AdminCandidateRejectRequest request
    ) {
        return ApiResponse.ok(candidateService.rejectCandidate(id, request));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<AdminCandidateResponse> publishCandidate(
            @PathVariable Long id,
            @RequestBody(required = false) AdminCandidatePublishRequest request
    ) {
        return ApiResponse.ok(candidateService.publishCandidate(id, request));
    }

    @PostMapping("/{id}/content/fetch")
    public ApiResponse<AdminCandidateContentResponse> fetchCandidateContent(
            @PathVariable Long id,
            @RequestBody(required = false) AdminCandidateContentFetchRequest request
    ) {
        AdminCandidateContentFetchRequest safeRequest = request == null
                ? new AdminCandidateContentFetchRequest(null)
                : request;
        return ApiResponse.ok(candidateService.fetchCandidateContent(id, safeRequest));
    }

    @PostMapping("/{id}/report-assets/{assetId}/cache")
    public ApiResponse<AdminReportAssetResponse> cacheReportAsset(
            @PathVariable Long id,
            @PathVariable Long assetId
    ) {
        return ApiResponse.ok(candidateService.cacheReportAsset(id, assetId));
    }

    @PostMapping("/{id}/report-assets/{assetId}/approve")
    public ApiResponse<AdminReportAssetResponse> approveReportAsset(
            @PathVariable Long id,
            @PathVariable Long assetId,
            @RequestBody(required = false) AdminReportAssetActionRequest request
    ) {
        return ApiResponse.ok(candidateService.approveReportAsset(id, assetId, request));
    }

    @PostMapping("/{id}/report-assets/{assetId}/reject")
    public ApiResponse<AdminReportAssetResponse> rejectReportAsset(
            @PathVariable Long id,
            @PathVariable Long assetId,
            @RequestBody(required = false) AdminReportAssetActionRequest request
    ) {
        return ApiResponse.ok(candidateService.rejectReportAsset(id, assetId, request));
    }
}
