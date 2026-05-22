package com.xtsh.ragchunk.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("document")
public class DocumentEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("kb_id")
    private String kbId;

    @TableField("file_name")
    private String fileName;

    @TableField("storage_key")
    private String storageKey;

    private String status;

    @TableField("chunk_count")
    private int chunkCount;

    private String profile;

    @TableField("quality_score")
    private int qualityScore;

    @TableField("ai_triggered")
    private boolean aiTriggered;

    @TableField("ai_trigger_id")
    private String aiTriggerId;

    @TableField("ai_fallback")
    private boolean aiFallback;

    @TableField("error_message")
    private String errorMessage;

    @TableField("batch_id")
    private String batchId;

    @TableField("process_stage")
    private String processStage;

    @TableField("progress_percent")
    private int progressPercent;

    @TableField("file_size")
    private long fileSize;

    @TableField("content_hash")
    private String contentHash;

    @TableField("storage_url")
    private String storageUrl;

    @TableField("source_type")
    private String sourceType;

    @TableField("retrain_version")
    private int retrainVersion;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKbId() { return kbId; }
    public void setKbId(String kbId) { this.kbId = kbId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
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
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getProcessStage() { return processStage; }
    public void setProcessStage(String processStage) { this.processStage = processStage; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getStorageUrl() { return storageUrl; }
    public void setStorageUrl(String storageUrl) { this.storageUrl = storageUrl; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public int getRetrainVersion() { return retrainVersion; }
    public void setRetrainVersion(int retrainVersion) { this.retrainVersion = retrainVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
