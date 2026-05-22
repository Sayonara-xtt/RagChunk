package com.xtsh.ragchunk.chunk.model;

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

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public boolean isWeakBoundary() { return weakBoundary; }
    public void setWeakBoundary(boolean weakBoundary) { this.weakBoundary = weakBoundary; }
    public int charLen() { return text != null ? text.length() : 0; }
}
