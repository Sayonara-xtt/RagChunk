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
}
