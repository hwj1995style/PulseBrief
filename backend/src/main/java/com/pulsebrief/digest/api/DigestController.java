package com.pulsebrief.digest.api;

import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.digest.service.DigestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/digests")
public class DigestController {
    private final DigestService digestService;

    public DigestController(DigestService digestService) {
        this.digestService = digestService;
    }

    @GetMapping("/today")
    public ApiResponse<TodayDigestResponse> todayDigest() {
        return ApiResponse.ok(digestService.getTodayDigest());
    }

    @GetMapping("/{id}")
    public ApiResponse<DigestDetailResponse> digestDetail(@PathVariable Long id) {
        return ApiResponse.ok(digestService.getDigestDetail(id));
    }
}
