package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.config.PdfCacheProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class HttpPdfDownloadClient implements PdfDownloadClient {
    private final PdfCacheProperties properties;
    private final HttpClient httpClient;

    public HttpPdfDownloadClient(PdfCacheProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(properties.getTimeoutSeconds(), 1)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public DownloadedPdf download(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("PDF URL must be public HTTP(S)");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(Math.max(properties.getTimeoutSeconds(), 1)))
                    .header("User-Agent", "PulseBriefPdfCache/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("PDF download failed with status " + response.statusCode());
            }
            byte[] bytes = response.body();
            if (bytes.length > properties.maxSizeBytes()) {
                throw new IllegalStateException("PDF file exceeds configured size limit");
            }
            String mimeType = response.headers()
                    .firstValue("content-type")
                    .map(value -> value.split(";", 2)[0].trim())
                    .filter(value -> !value.isBlank())
                    .orElse("application/pdf");
            if (!isPdfMime(mimeType) && !hasPdfSignature(bytes)) {
                throw new IllegalStateException("Downloaded file is not a PDF");
            }
            return new DownloadedPdf(fileName(uri), mimeType, bytes);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PDF download was interrupted", exception);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("PDF download failed: " + exception.getMessage(), exception);
        }
    }

    private boolean isPdfMime(String mimeType) {
        return "application/pdf".equalsIgnoreCase(mimeType) || mimeType.toLowerCase().contains("pdf");
    }

    private boolean hasPdfSignature(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F';
    }

    private String fileName(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank() || path.endsWith("/")) {
            return "report.pdf";
        }
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.isBlank() ? "report.pdf" : name;
    }
}
