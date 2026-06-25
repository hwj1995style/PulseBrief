package com.pulsebrief.ingestion.service;

public record DownloadedPdf(
        String fileName,
        String mimeType,
        byte[] bytes
) {
}
