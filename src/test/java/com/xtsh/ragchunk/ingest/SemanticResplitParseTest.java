package com.xtsh.ragchunk.ingest;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class SemanticResplitParseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesJsonFromMarkdownFence() throws Exception {
        String raw = """
                ```json
                {"chunks":[{"text":"第一段内容"},{"text":"第二段内容"}]}
                ```
                """;
        String json = extractJsonLikeProduction(raw);
        JsonNode arr = mapper.readTree(json).path("chunks");
        assertEquals(2, arr.size());
    }

    @Test
    void parsesJsonWithLeadingText() throws Exception {
        String raw = "这是说明\n{\"chunks\":[{\"text\":\"only\"}]}";
        String json = extractJsonLikeProduction(raw);
        assertTrue(json.startsWith("{"));
        assertEquals(1, mapper.readTree(json).path("chunks").size());
    }

    private static String extractJsonLikeProduction(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(?:json)?\\s*", "");
            int endFence = s.lastIndexOf("```");
            if (endFence >= 0) {
                s = s.substring(0, endFence).trim();
            }
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        throw new IllegalStateException("no JSON");
    }
}
