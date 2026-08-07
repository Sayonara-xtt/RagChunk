package com.xtsh.ragchunk.vo.document;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "异步/批量上传受理响应（立即返回，后台处理）")
@Data

public class AsyncUploadResponse {

    @Schema(description = "文档 ID")
    private String docId;
    @Schema(description = "批量任务 ID，单文件可为空")
    private String batchId;
    @Schema(description = "粗粒度状态", example = "QUEUED")
    private String status;
    @Schema(description = "流程阶段", example = "QUEUED")
    private String processStage;
    @Schema(description = "提示信息")
    private String message;

    public static AsyncUploadResponse forDocument(String docId, String batchId, String status, String processStage) {
        var r = new AsyncUploadResponse();
        r.docId = docId;
        r.batchId = batchId;
        r.status = status;
        r.processStage = processStage;
        r.message = "accepted, processing in background; poll document or batch API for progress";
        return r;
    }

}
