package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.TextChunk;

import java.util.List;
import java.util.stream.Collectors;

/** 上传/入库日志中的正文预览，避免刷屏。 */
final class IngestLogSupport {

    static final int TEXT_PREVIEW_CHARS = 300;
    static final int RESPONSE_PREVIEW_CHARS = 500;

    private IngestLogSupport() {}

    static String previewText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String oneLine = text.replace('\r', ' ').replace('\n', ' ').trim();
        if (oneLine.length() <= TEXT_PREVIEW_CHARS) {
            return oneLine;
        }
        return oneLine.substring(0, TEXT_PREVIEW_CHARS) + "...(共" + text.length() + "字)";
    }

    static String previewResponse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String oneLine = raw.replace('\r', ' ').replace('\n', ' ').trim();
        if (oneLine.length() <= RESPONSE_PREVIEW_CHARS) {
            return oneLine;
        }
        return oneLine.substring(0, RESPONSE_PREVIEW_CHARS) + "...(共" + raw.length() + "字)";
    }

    static String previewChunks(List<TextChunk> chunks, int maxItems) {
        if (chunks == null || chunks.isEmpty()) {
            return "[]";
        }
        return chunks.stream()
                .limit(maxItems)
                .map(c -> "[" + c.getIndex() + "]" + previewText(c.getText()))
                .collect(Collectors.joining(" | "));
    }
}
