package com.pulsebrief.playback.service;

import com.pulsebrief.playback.api.PlaybackHistoryItemResponse;
import com.pulsebrief.playback.api.PlaybackHistoryRequest;
import com.pulsebrief.playback.api.PlaybackHistoryResponse;
import java.util.List;

public interface PlaybackService {
    PlaybackHistoryResponse recordPlayback(Long userId, PlaybackHistoryRequest request);

    List<PlaybackHistoryItemResponse> listPlaybackHistory(Long userId, Integer page, Integer pageSize);
}
