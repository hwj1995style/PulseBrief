package com.pulsebrief.ingestion.service;

@FunctionalInterface
public interface PdfDownloadClient {
    DownloadedPdf download(String url);
}
