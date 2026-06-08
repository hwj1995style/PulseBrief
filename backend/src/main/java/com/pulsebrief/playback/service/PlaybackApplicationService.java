package com.pulsebrief.playback.service;

import com.pulsebrief.playback.api.PlaybackHistoryRequest;
import com.pulsebrief.playback.api.PlaybackHistoryResponse;
import com.pulsebrief.playback.domain.UserPlayHistory;
import com.pulsebrief.playback.repository.UserPlayHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class PlaybackApplicationService implements PlaybackService {
    private final UserPlayHistoryRepository playHistoryRepository;

    public PlaybackApplicationService(UserPlayHistoryRepository playHistoryRepository) {
        this.playHistoryRepository = playHistoryRepository;
    }

    @Override
    public PlaybackHistoryResponse recordPlayback(Long userId, PlaybackHistoryRequest request) {
        UserPlayHistory saved = playHistoryRepository.save(new UserPlayHistory(
                userId,
                request.articleId(),
                request.digestId(),
                request.playType(),
                request.playTitle(),
                request.durationSeconds()
        ));
        return new PlaybackHistoryResponse(saved.getId());
    }
}
