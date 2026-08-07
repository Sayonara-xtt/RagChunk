package com.xtsh.ragchunk.dto.chunk;

import lombok.Data;
@Data

public class TextChunk {
    private int index;
    private String text;
    private String source = "rule";
    private boolean weakBoundary;

    public TextChunk() {}

    public TextChunk(int index, String text) {
        this.index = index;
        this.text = text;
    }

    public int charLen() { return text != null ? text.length() : 0; }
}
