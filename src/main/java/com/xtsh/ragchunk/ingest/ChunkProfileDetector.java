package com.xtsh.ragchunk.ingest;

import org.springframework.stereotype.Component;

@Component
public class ChunkProfileDetector {

    public static final String MARKDOWN = "markdown";
    public static final String PLAIN = "plain";

    public String detect(String fileName) {
        if (fileName != null) {
            var lower = fileName.toLowerCase();
            if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
                return MARKDOWN;
            }
        }
        return PLAIN;
    }
}
