package com.pulsebrief.playback.api;

public record PlaybackHistoryItemResponse(
        Long id,
        String playType,
        Long articleId,
        Long digestId,
        String playTitle,
        String playTime,
        Integer durationSeconds
) {
}
