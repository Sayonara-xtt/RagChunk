package com.xtsh.ragchunk.dto.document;

import lombok.Data;
import java.time.Instant;

@Data

public class UploadBatchRecord {
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
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

}
