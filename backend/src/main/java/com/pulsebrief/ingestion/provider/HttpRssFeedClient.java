package com.pulsebrief.ingestion.provider;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpRssFeedClient implements RssFeedClient {
    private final RestClient restClient;

    public HttpRssFeedClient(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(15_000);
        configuredProxy().ifPresent(requestFactory::setProxy);
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String fetch(String feedUrl) {
        return restClient.get()
                .uri(feedUrl)
                .retrieve()
                .body(String.class);
    }

    private Optional<Proxy> configuredProxy() {
        String proxyUrl = firstPresentEnv("HTTPS_PROXY", "https_proxy", "HTTP_PROXY", "http_proxy");
        if (proxyUrl == null || proxyUrl.isBlank()) {
            return Optional.empty();
        }

        try {
            URI proxyUri = URI.create(proxyUrl.contains("://") ? proxyUrl : "http://" + proxyUrl);
            String host = proxyUri.getHost();
            if (host == null || host.isBlank()) {
                return Optional.empty();
            }
            int port = proxyUri.getPort();
            if (port <= 0) {
                port = "https".equals(proxyUri.getScheme().toLowerCase(Locale.ROOT)) ? 443 : 80;
            }
            return Optional.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String firstPresentEnv(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
