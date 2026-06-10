package com.pulsebrief.admin.api;

import com.pulsebrief.admin.service.AdminDigestApplicationService;
import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.common.api.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/digests")
@Tag(name = "Admin")
public class AdminDigestController {
    private final AdminDigestApplicationService digestService;

    public AdminDigestController(AdminDigestApplicationService digestService) {
        this.digestService = digestService;
    }

    @GetMapping("/article-candidates")
    public ApiResponse<PageResponse<ArticleCardResponse>> articleCandidates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(digestService.listArticleCandidates(keyword, categoryCode, page, pageSize));
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminDigestResponse>> listDigests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(digestService.listDigests(status, page, pageSize));
    }

    @PostMapping
    public ApiResponse<AdminDigestResponse> createDigest(@RequestBody AdminDigestCreateRequest request) {
        return ApiResponse.ok(digestService.createDigest(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminDigestResponse> getDigest(@PathVariable Long id) {
        return ApiResponse.ok(digestService.getDigest(id));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<AdminDigestResponse> publishDigest(
            @PathVariable Long id,
            @RequestBody(required = false) AdminDigestPublishRequest request
    ) {
        return ApiResponse.ok(digestService.publishDigest(id));
    }
}
