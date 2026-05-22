package com.xtsh.ragchunk.knowledge.model;

import java.time.Instant;

public class KnowledgeBase {
    private String id;
    private String name;
    private String description;
    private String status;
    private KnowledgeBaseConfig config;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public KnowledgeBaseConfig getConfig() { return config; }
    public void setConfig(KnowledgeBaseConfig config) { this.config = config; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
