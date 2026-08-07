package com.xtsh.ragchunk.vo.chunk;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "切片（不含向量）")
@Data

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

}
