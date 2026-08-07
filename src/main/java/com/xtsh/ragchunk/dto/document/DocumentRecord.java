package com.xtsh.ragchunk.dto.document;

import lombok.Data;
import java.time.Instant;

@Data

public class DocumentRecord {
    private String id;
    private String kbId;
    private String fileName;
    private String status;
    private int chunkCount;
    private String profile;
    private int qualityScore;
    private boolean aiTriggered;
    private String aiTriggerId;
    private boolean aiFallback;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    private String batchId;
    private String processStage;
    private int progressPercent;
    private long fileSize;
    private String storageKey;
    private String storageUrl;
    private String contentHash;
    private String sourceType;
    private int retrainVersion;

}
