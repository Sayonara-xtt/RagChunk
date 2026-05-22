package com.xtsh.ragchunk.document;

import com.xtsh.ragchunk.document.dto.AsyncUploadResponse;
import com.xtsh.ragchunk.document.dto.DocumentResponse;
import com.xtsh.ragchunk.document.dto.DocumentUploadResponse;
import com.xtsh.ragchunk.document.model.UploadSourceType;
import com.xtsh.ragchunk.web.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "文档", description = "文档异步入库、流程查询、重复训练")
@RestController
@RequestMapping("/api/v1/knowledge-bases/{kbId}/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final DocumentAsyncUploadService asyncUploadService;

    public DocumentController(DocumentService documentService, DocumentAsyncUploadService asyncUploadService) {
        this.documentService = documentService;
        this.asyncUploadService = asyncUploadService;
    }

    @Operation(summary = "上传文档（异步 + 流式）",
            description = "一律 202：请求线程流式写入原件存储（不占满堆），再后台解析→切片→向量。"
                    + "单文件 file，多文件 files；轮询 GET .../documents/{docId}。")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "已入队",
                    content = @Content(schema = @Schema(implementation = DocumentUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "参数或文件错误"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentUploadResponse upload(
            @Parameter(description = "知识库 ID", required = true, example = "kb_a1b2c3d4e5f6")
            @PathVariable String kbId,
            @Parameter(description = "单文件（与 files 二选一或仅传其一）")
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Parameter(description = "多文件批量")
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @Parameter(description = "true=强制千问语义重切(T8)", example = "false")
            @RequestParam(value = "smartChunk", defaultValue = "false") boolean smartChunk,
            @Parameter(description = "单文件默认 API_SINGLE；多文件默认 LOCAL_BATCH。可选 LOCAL_BATCH / OSS_SERVER_BATCH")
            @RequestParam(value = "sourceType", required = false) String sourceType) {
        List<MultipartFile> merged = mergeFiles(file, files);
        UploadSourceType src = resolveSourceType(merged.size(), sourceType);
        log.info("[文档上传] 异步入队 kbId={}, 文件数={}, sourceType={}, smartChunk={}",
                kbId, merged.size(), src, smartChunk);
        return asyncUploadService.submit(kbId, merged, smartChunk, src);
    }

    @Operation(summary = "重复训练", description = "基于 OSS 已归档原件重新切片并向量化（递增 retrainVersion）")
    @PostMapping("/{docId}/retrain")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AsyncUploadResponse retrain(
            @PathVariable String kbId,
            @PathVariable String docId,
            @RequestParam(value = "smartChunk", defaultValue = "false") boolean smartChunk) {
        log.info("[文档上传] 重复训练 kbId={}, docId={}", kbId, docId);
        return asyncUploadService.retrain(kbId, docId, smartChunk);
    }

    @Operation(summary = "文档列表", description = "含 processStage、进度、OSS 等；上传完成后 status=SUCCESS")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentResponse.class))))
    @GetMapping
    public List<DocumentResponse> list(@PathVariable String kbId) {
        return documentService.list(kbId);
    }

    @Operation(summary = "查询文档详情与流程进度")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "404", description = "知识库或文档不存在")
    })
    @GetMapping("/{docId}")
    public DocumentResponse get(@PathVariable String kbId, @PathVariable String docId) {
        return documentService.get(kbId, docId);
    }

    private static List<MultipartFile> mergeFiles(MultipartFile file, List<MultipartFile> files) {
        var merged = new ArrayList<MultipartFile>();
        if (files != null) {
            merged.addAll(files.stream().filter(f -> f != null && !f.isEmpty()).toList());
        }
        if (file != null && !file.isEmpty()) {
            merged.add(file);
        }
        if (merged.isEmpty()) {
            throw new BadRequestException("file or files is required");
        }
        return merged;
    }

    private static UploadSourceType resolveSourceType(int fileCount, String raw) {
        if (raw != null && !raw.isBlank()) {
            try {
                return UploadSourceType.valueOf(raw);
            } catch (Exception e) {
                throw new BadRequestException("sourceType must be API_SINGLE, LOCAL_BATCH or OSS_SERVER_BATCH");
            }
        }
        return fileCount > 1 ? UploadSourceType.LOCAL_BATCH : UploadSourceType.API_SINGLE;
    }
}
