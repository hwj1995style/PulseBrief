package com.pulsebrief.ingestion.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class HttpHtmlContentClient implements HtmlContentClient {
    private static final int MAX_RESPONSE_CHARS = 1_000_000;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "PulseBriefContentFetcher/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Content fetch returned HTTP " + response.statusCode());
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            if (!contentType.isBlank() && !contentType.toLowerCase().contains("text/html")) {
                throw new IllegalStateException("Content fetch only supports text/html responses");
            }
            String body = response.body() == null ? "" : response.body();
            if (body.length() > MAX_RESPONSE_CHARS) {
                throw new IllegalStateException("Content fetch response is too large");
            }
            return body;
        } catch (IOException exception) {
            throw new IllegalStateException("Content fetch failed: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Content fetch interrupted", exception);
        }
    }
}
