package com.pulsebrief.readhistory.api;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.common.security.DevTokenSupport;
import com.pulsebrief.readhistory.service.ReadHistoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/read-history")
public class ReadHistoryController {
    private final ReadHistoryService readHistoryService;

    public ReadHistoryController(ReadHistoryService readHistoryService) {
        this.readHistoryService = readHistoryService;
    }

    @PostMapping
    public ApiResponse<ReadHistoryRecordResponse> recordReadHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ReadHistoryRecordRequest request
    ) {
        Long userId = DevTokenSupport.requireUserId(authorization);
        return ApiResponse.ok(readHistoryService.recordReadHistory(userId, request.articleId()));
    }

    @GetMapping
    public ApiResponse<List<ArticleCardResponse>> readHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        Long userId = DevTokenSupport.requireUserId(authorization);
        return ApiResponse.ok(readHistoryService.listReadHistory(userId, page, pageSize));
    }
}
