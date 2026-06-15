package com.pulsebrief.ingestion.provider;

public class RssFeedParseException extends RuntimeException {
    public RssFeedParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
