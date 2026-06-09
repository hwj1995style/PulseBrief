package com.pulsebrief.ingestion.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class IngestionDeduplicationService {
    public String urlHash(String originalUrl) {
        return sha256(normalize(originalUrl));
    }

    public String contentHash(String sourceName, String title, OffsetDateTime publishedAt) {
        String publishDate = publishedAt == null ? "" : publishedAt.toLocalDate().toString();
        return sha256(normalize(sourceName) + "|" + normalize(title) + "|" + publishDate);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
