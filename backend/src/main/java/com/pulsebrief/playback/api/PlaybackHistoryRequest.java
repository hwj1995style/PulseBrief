package com.pulsebrief.playback.api;

public record PlaybackHistoryRequest(
        String playType,
        Long articleId,
        Long digestId,
        String playTitle,
        Integer durationSeconds
) {
}
