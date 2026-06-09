package com.pulsebrief.playback.api;

import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.common.api.ClearResponse;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.common.security.DevTokenSupport;
import com.pulsebrief.playback.service.PlaybackService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playback/history")
public class PlaybackController {
    private final PlaybackService playbackService;

    public PlaybackController(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @PostMapping
    public ApiResponse<PlaybackHistoryResponse> recordPlayback(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody PlaybackHistoryRequest request
    ) {
        Long userId = DevTokenSupport.requireUserId(authorization);
        return ApiResponse.ok(playbackService.recordPlayback(userId, request));
    }

    @GetMapping
    public ApiResponse<PageResponse<PlaybackHistoryItemResponse>> playbackHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        Long userId = DevTokenSupport.requireUserId(authorization);
        return ApiResponse.ok(playbackService.listPlaybackHistory(userId, page, pageSize));
    }

    @DeleteMapping
    public ApiResponse<ClearResponse> clearPlaybackHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = DevTokenSupport.requireUserId(authorization);
        playbackService.clearPlaybackHistory(userId);
        return ApiResponse.ok(ClearResponse.ok());
    }
}
