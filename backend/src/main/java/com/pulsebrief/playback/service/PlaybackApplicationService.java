package com.pulsebrief.playback.service;

import com.pulsebrief.playback.api.PlaybackHistoryItemResponse;
import com.pulsebrief.playback.api.PlaybackHistoryRequest;
import com.pulsebrief.playback.api.PlaybackHistoryResponse;
import com.pulsebrief.playback.domain.UserPlayHistory;
import com.pulsebrief.playback.repository.UserPlayHistoryRepository;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PlaybackApplicationService implements PlaybackService {
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

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

    @Override
    public List<PlaybackHistoryItemResponse> listPlaybackHistory(Long userId, Integer page, Integer pageSize) {
        PageRequest pageable = PageRequest.of(safePage(page), safePageSize(pageSize));
        return playHistoryRepository.findByUserIdOrderByPlayTimeDesc(userId, pageable)
                .stream()
                .map(this::toHistoryItem)
                .toList();
    }

    private PlaybackHistoryItemResponse toHistoryItem(UserPlayHistory history) {
        return new PlaybackHistoryItemResponse(
                history.getId(),
                history.getPlayType(),
                history.getArticleId(),
                history.getDigestId(),
                history.getPlayTitle(),
                history.getPlayTime() == null ? null : history.getPlayTime().atOffset(ZoneOffset.ofHours(8)).format(API_TIME),
                history.getDurationSeconds()
        );
    }

    private int safePage(Integer page) {
        return Math.max(page == null ? 1 : page, 1) - 1;
    }

    private int safePageSize(Integer pageSize) {
        return Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
    }
}
