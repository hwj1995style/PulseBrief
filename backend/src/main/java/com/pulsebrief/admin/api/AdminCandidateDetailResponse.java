package com.pulsebrief.admin.api;

import java.util.List;

public record AdminCandidateDetailResponse(
        AdminCandidateResponse candidate,
        AdminRawNewsItemResponse rawItem,
        List<AdminReportAssetResponse> reportAssets,
        List<String> duplicateHints,
        List<String> availableActions
) {
}
