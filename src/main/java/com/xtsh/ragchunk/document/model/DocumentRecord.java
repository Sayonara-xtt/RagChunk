package com.xtsh.ragchunk.document.model;

import java.time.Instant;

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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKbId() { return kbId; }
    public void setKbId(String kbId) { this.kbId = kbId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }
    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
    public boolean isAiTriggered() { return aiTriggered; }
    public void setAiTriggered(boolean aiTriggered) { this.aiTriggered = aiTriggered; }
    public String getAiTriggerId() { return aiTriggerId; }
    public void setAiTriggerId(String aiTriggerId) { this.aiTriggerId = aiTriggerId; }
    public boolean isAiFallback() { return aiFallback; }
    public void setAiFallback(boolean aiFallback) { this.aiFallback = aiFallback; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getProcessStage() { return processStage; }
    public void setProcessStage(String processStage) { this.processStage = processStage; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getStorageUrl() { return storageUrl; }
    public void setStorageUrl(String storageUrl) { this.storageUrl = storageUrl; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public int getRetrainVersion() { return retrainVersion; }
    public void setRetrainVersion(int retrainVersion) { this.retrainVersion = retrainVersion; }
}
