package com.pulsebrief.playback.service;

import com.pulsebrief.playback.api.PlaybackHistoryRequest;
import com.pulsebrief.playback.api.PlaybackHistoryResponse;

public interface PlaybackService {
    PlaybackHistoryResponse recordPlayback(Long userId, PlaybackHistoryRequest request);
}
