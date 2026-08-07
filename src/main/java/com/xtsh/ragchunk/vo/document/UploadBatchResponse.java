package com.xtsh.ragchunk.vo.document;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "批量上传任务状态")
@Data

public class UploadBatchResponse {

    private String id;
    private String kbId;
    private String sourceType;
    private String status;
    private int totalCount;
    private int queuedCount;
    private int processingCount;
    private int successCount;
    private int failedCount;
    private boolean smartChunk;
    private List<String> documentIds;
    private Instant createdAt;
    private Instant updatedAt;

}
