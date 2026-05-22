package com.xtsh.ragchunk.chunk;

import com.xtsh.ragchunk.chunk.dto.ChunkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "切片", description = "查询已入库的文档切片（不含向量）")
@RestController
@RequestMapping("/api/v1/knowledge-bases/{kbId}")
public class ChunkController {

    private final ChunkService chunkService;

    public ChunkController(ChunkService chunkService) {
        this.chunkService = chunkService;
    }

    @Operation(summary = "查询切片", description = "传 chunkId：返回单个；传 docId：返回该文档下全部切片；都不传：返回知识库下全部切片")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ChunkResponse.class))),
            @ApiResponse(responseCode = "404", description = "知识库/文档/切片不存在")
    })
    @GetMapping("/chunks")
    public Object query(
            @Parameter(description = "知识库 ID", required = true, example = "kb_a1b2c3d4e5f6")
            @PathVariable String kbId,
            @Parameter(description = "文档 ID，按文档过滤")
            @RequestParam(value = "docId", required = false) String docId,
            @Parameter(description = "切片 ID，查询单个")
            @RequestParam(value = "id", required = false) String chunkId) {
        return chunkService.query(kbId, docId, chunkId);
    }

    @Operation(summary = "文档切片列表",
            description = "docId 可选：不传或为空时返回知识库下全部切片（按 doc_id、chunk_index 排序）；传 docId 时返回该文档下切片（按 chunk_index 升序）")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChunkResponse.class))))
    @GetMapping({"/documents/chunks", "/documents/{docId}/chunks"})
    public List<ChunkResponse> listByDocument(
            @Parameter(description = "知识库 ID", required = true, example = "kb_a1b2c3d4e5f6")
            @PathVariable String kbId,
            @Parameter(description = "文档 ID；不传则返回知识库全部切片", example = "doc_a1b2c3d4e5f6")
            @PathVariable(required = false) String docId) {
        return chunkService.listByDocument(kbId, docId);
    }
}
