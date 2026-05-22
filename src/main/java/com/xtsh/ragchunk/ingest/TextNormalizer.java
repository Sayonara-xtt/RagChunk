package com.xtsh.ragchunk.ingest;

import org.springframework.stereotype.Component;

@Component
public class TextNormalizer {

    public String normalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String t = raw.replace("\r\n", "\n").replace('\r', '\n');
        t = t.replaceAll("[ \t]+", " ");
        t = t.replaceAll("\n{3,}", "\n\n");
        var lines = t.split("\n", -1);
        var sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines[i].trim());
        }
        return sb.toString().trim();
    }
}
