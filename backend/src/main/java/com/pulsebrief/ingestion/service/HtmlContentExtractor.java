package com.pulsebrief.ingestion.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class HtmlContentExtractor {
    private static final Pattern NON_CONTENT_BLOCKS = Pattern.compile(
            "(?is)<(script|style|nav|footer|header|aside)[^>]*>.*?</\\1>"
    );
    private static final Pattern TAGS = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public String extract(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String cleaned = NON_CONTENT_BLOCKS.matcher(html).replaceAll(" ");
        cleaned = cleaned.replaceAll("(?is)</p>", "\n");
        cleaned = cleaned.replaceAll("(?is)<br\\s*/?>", "\n");
        cleaned = TAGS.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
    }
}
