package com.xtsh.ragchunk.chunk.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "切片（不含向量）")
public class ChunkResponse {

    @Schema(description = "切片 ID", example = "doc_abc_c0000")
    private String id;

    @Schema(description = "知识库 ID", example = "kb_a1b2c3d4e5f6")
    private String kbId;

    @Schema(description = "文档 ID", example = "doc_a1b2c3d4e5f6")
    private String docId;

    @Schema(description = "文档内序号，从 0 起", example = "0")
    private int chunkIndex;

    @Schema(description = "切片正文")
    private String textContent;

    @Schema(description = "来源：规则切片或 hybrid（千问）", example = "hybrid")
    private String source;

    @Schema(description = "入库时间（UTC）")
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKbId() { return kbId; }
    public void setKbId(String kbId) { this.kbId = kbId; }
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
