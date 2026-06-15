package com.pulsebrief.ingestion.provider;

@FunctionalInterface
public interface RssFeedClient {
    String fetch(String feedUrl);
}
