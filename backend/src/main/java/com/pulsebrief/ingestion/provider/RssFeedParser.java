package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.service.IngestionRequest;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RssFeedParser {
    public List<RawNewsPayload> parse(String feedXml, IngestionRequest request) {
        try {
            SyndFeed feed = new SyndFeedInput().build(new StringReader(feedXml));
            int pageSize = request.pageSizeOrDefault();
            return feed.getEntries().stream()
                    .map(entry -> toPayload(feed, entry, request))
                    .flatMap(Optional::stream)
                    .filter(payload -> matchesKeywords(payload, request.keywords()))
                    .limit(pageSize)
                    .toList();
        } catch (FeedException e) {
            throw new RssFeedParseException("Unable to parse RSS or Atom feed", e);
        }
    }

    private Optional<RawNewsPayload> toPayload(SyndFeed feed, SyndEntry entry, IngestionRequest request) {
        String title = normalizeText(entry.getTitle());
        String originalUrl = normalizeText(entry.getLink());
        if (title == null || originalUrl == null) {
            return Optional.empty();
        }

        String providerItemId = normalizeText(entry.getUri());
        if (providerItemId == null) {
            providerItemId = originalUrl;
        }

        String summary = summary(entry);
        String sourceName = normalizeText(feed.getTitle());
        if (sourceName == null) {
            sourceName = request.sourceCode();
        }

        return Optional.of(new RawNewsPayload(
                providerItemId,
                title,
                summary,
                sourceName,
                originalUrl,
                null,
                publishedAt(entry),
                request.language(),
                request.country(),
                rawPayload(providerItemId, title, originalUrl, sourceName)
        ));
    }

    private String summary(SyndEntry entry) {
        SyndContent description = entry.getDescription();
        if (description == null) {
            return null;
        }
        return normalizeText(description.getValue());
    }

    private OffsetDateTime publishedAt(SyndEntry entry) {
        Date publishedDate = entry.getPublishedDate();
        if (publishedDate == null) {
            publishedDate = entry.getUpdatedDate();
        }
        if (publishedDate == null) {
            return null;
        }
        return publishedDate.toInstant().atOffset(ZoneOffset.UTC);
    }

    private boolean matchesKeywords(RawNewsPayload payload, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        String searchable = (payload.title() + " " + nullToEmpty(payload.summary())).toLowerCase(Locale.ROOT);
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT).trim())
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(searchable::contains);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String text = value
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .replaceAll("\\s+([.,;:!?])", "$1")
                .trim();
        return text.isBlank() ? null : text;
    }

    private String rawPayload(String providerItemId, String title, String originalUrl, String sourceName) {
        return "{\"providerItemId\":\"" + escapeJson(providerItemId)
                + "\",\"title\":\"" + escapeJson(title)
                + "\",\"originalUrl\":\"" + escapeJson(originalUrl)
                + "\",\"sourceName\":\"" + escapeJson(sourceName)
                + "\"}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
