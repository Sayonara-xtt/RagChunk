package com.xtsh.ragchunk.vo.document;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 文档上传受理响应（一律异步，HTTP 202）。
 * <ul>
 *   <li>{@code kind=single}：单文件，看 {@code docId}</li>
 *   <li>{@code kind=batch}：多文件，看 {@code batchId} 与 {@code documentIds}</li>
 * </ul>
 */
@Schema(description = "文档上传受理（异步后台处理）")
@Data

public class DocumentUploadResponse {

    @Schema(description = "single | batch", example = "single")
    private String kind;

    @Schema(description = "单文件时的文档 ID")
    private String docId;

    @Schema(description = "批量任务 ID，单文件为空")
    private String batchId;

    @Schema(description = "已入队文档 ID 列表")
    private List<String> documentIds;

    @Schema(description = "粗粒度状态", example = "QUEUED")
    private String status;

    @Schema(description = "流程阶段", example = "QUEUED")
    private String processStage;

    @Schema(description = "批量：文件总数（仅 kind=batch）")
    private Integer totalCount;

    @Schema(description = "提示信息")
    private String message;

    public static DocumentUploadResponse single(String docId, String status, String processStage) {
        var r = new DocumentUploadResponse();
        r.kind = "single";
        r.docId = docId;
        r.documentIds = List.of(docId);
        r.status = status;
        r.processStage = processStage;
        r.message = "accepted; poll GET .../documents/{docId} for progress";
        return r;
    }

    public static DocumentUploadResponse batch(UploadBatchResponse batch) {
        var r = new DocumentUploadResponse();
        r.kind = "batch";
        r.batchId = batch.getId();
        r.documentIds = batch.getDocumentIds();
        r.status = batch.getStatus();
        r.processStage = "QUEUED";
        r.totalCount = batch.getTotalCount();
        r.message = "batch accepted; poll GET .../upload-batches/{batchId} or upload-dashboard";
        return r;
    }

}
