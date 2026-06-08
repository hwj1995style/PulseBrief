package com.pulsebrief.digest.service;

import com.pulsebrief.digest.api.DigestDetailResponse;
import com.pulsebrief.digest.api.DigestSummaryResponse;
import com.pulsebrief.digest.api.TodayDigestResponse;
import com.pulsebrief.digest.domain.DailyDigest;
import com.pulsebrief.digest.repository.DigestRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class DigestQueryService implements DigestService {
    private static final String PUBLISHED = "PUBLISHED";
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final DigestRepository digestRepository;

    public DigestQueryService(DigestRepository digestRepository) {
        this.digestRepository = digestRepository;
    }

    @Override
    public TodayDigestResponse getTodayDigest() {
        DailyDigest headline = digestRepository.findFirstByDigestStatusOrderByDigestDateDescPublishTimeAsc(PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Digest not found"));
        List<DailyDigest> digests = digestRepository.findByDigestDateAndDigestStatusOrderByPublishTimeAscIdAsc(
                headline.getDigestDate(),
                PUBLISHED
        );
        return new TodayDigestResponse(
                headline.getDigestDate().toString(),
                toSummary(headline),
                digests.stream().map(this::toSummary).toList(),
                parseLines(headline.getContent()).stream().limit(6).toList()
        );
    }

    @Override
    public DigestDetailResponse getDigestDetail(Long id) {
        DailyDigest digest = digestRepository.findByIdAndDigestStatus(id, PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Digest not found"));
        return new DigestDetailResponse(
                digest.getId(),
                digest.getTitle(),
                "脉闻语音简报",
                digest.getDigestType(),
                durationFor(digest.getDigestType()),
                formatTime(digest),
                digest.getSummary(),
                digest.getAudioText(),
                parseLines(digest.getContent())
        );
    }

    private DigestSummaryResponse toSummary(DailyDigest digest) {
        return new DigestSummaryResponse(
                digest.getId(),
                digest.getTitle(),
                digest.getSummary(),
                durationFor(digest.getDigestType()),
                updateLabel(digest)
        );
    }

    private List<String> parseLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private String formatTime(DailyDigest digest) {
        if (digest.getPublishTime() == null) {
            return LocalDate.now().atStartOfDay().atOffset(ZoneOffset.ofHours(8)).format(API_TIME);
        }
        return digest.getPublishTime().atOffset(ZoneOffset.ofHours(8)).format(API_TIME);
    }

    private String updateLabel(DailyDigest digest) {
        if (digest.getPublishTime() == null) {
            return "今日更新";
        }
        return digest.getPublishTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " 更新";
    }

    private String durationFor(String digestType) {
        return switch (digestType) {
            case "NOON" -> "04:36";
            case "EVENING" -> "07:28";
            case "AI" -> "06:10";
            case "INVESTMENT_VIEW" -> "05:48";
            default -> "08:12";
        };
    }
}
