package com.xtsh.ragchunk.document;

import com.xtsh.ragchunk.document.dto.DocumentResponse;
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

import java.util.List;

@Tag(name = "文档", description = "上传文档并触发离线建库：解析→ 混合切片 → 向量入库")
@RestController
@RequestMapping("/api/v1/knowledge-bases/{kbId}/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "上传文档", description = "multipart 同步建库：解析→混合切片→向量入库；成功时 status=SUCCESS")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "上传并入库成功",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "文件类型不支持或参数错误"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    /**
     * 上传文档（同步离线建库，一次请求内完成全流程）。
     * <ul>
     *   <li>表单字段 {@code file} 必填，支持 txt / md / docx / xlsx / xls</li>
     *   <li>{@code smartChunk=true} 在 aiMode≠never 时强制千问重切（T8）</li>
     *   <li>切片/向量规则读知识库 {@code config_json} 快照，非全局 application.yaml</li>
     * </ul>
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @Parameter(description = "知识库 ID", required = true, example = "kb_a1b2c3d4e5f6")
            @PathVariable String kbId,
            @Parameter(description = "文档文件（txt/md/docx/xlsx/xls）", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "true=强制千问语义重切(T8)；false=按库配置 aiMode 自动判断", example = "false")
            @RequestParam(value = "smartChunk", defaultValue = "false") boolean smartChunk) {
        log.info("[文档上传] 接口请求 kbId={}, 文件={}, 大小={}B, smartChunk={}（true 且库 aiMode≠never 时强制 AI 重切 T8）",
                kbId, file.getOriginalFilename(), file.getSize(), smartChunk);
        DocumentResponse resp = documentService.upload(kbId, file, smartChunk);
        log.info("[文档上传] 接口响应 docId={}, 状态={}, 切片数={}, 画像={}, 质量分={}, "
                        + "AI已触发={}, 触发ID={}, AI回退={}, 失败原因={}",
                resp.getId(), resp.getStatus(), resp.getChunkCount(), resp.getProfile(), resp.getQualityScore(),
                resp.isAiTriggered(), resp.getAiTriggerId(), resp.isAiFallback(), resp.getErrorMessage());
        return resp;
    }

    @Operation(summary = "文档列表", description = "返回指定知识库下已上传的全部文档")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentResponse.class))))
    @GetMapping
    public List<DocumentResponse> list(
            @Parameter(description = "知识库 ID", required = true, example = "kb_a1b2c3d4e5f6")
            @PathVariable String kbId) {
        return documentService.list(kbId);
    }

    @Operation(summary = "查询文档详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "404", description = "知识库或文档不存在")
    })
    @GetMapping("/{docId}")
    public DocumentResponse get(
            @Parameter(description = "知识库 ID", required = true, example = "kb_a1b2c3d4e5f6")
            @PathVariable String kbId,
            @Parameter(description = "文档 ID", required = true, example = "doc_a1b2c3d4e5f6")
            @PathVariable String docId) {
        return documentService.get(kbId, docId);
    }
}
