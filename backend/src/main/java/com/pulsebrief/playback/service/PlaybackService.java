package com.pulsebrief.playback.service;

import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.playback.api.PlaybackHistoryItemResponse;
import com.pulsebrief.playback.api.PlaybackHistoryRequest;
import com.pulsebrief.playback.api.PlaybackHistoryResponse;

public interface PlaybackService {
    PlaybackHistoryResponse recordPlayback(Long userId, PlaybackHistoryRequest request);

    PageResponse<PlaybackHistoryItemResponse> listPlaybackHistory(Long userId, Integer page, Integer pageSize);

    Boolean clearPlaybackHistory(Long userId);
}
