package com.xtsh.ragchunk.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.document.model.DocumentRecord;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBase;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import com.xtsh.ragchunk.persistence.entity.DocumentEntity;
import com.xtsh.ragchunk.persistence.entity.KnowledgeBaseEntity;
import com.xtsh.ragchunk.web.BadRequestException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
public class PersistenceMapper {

    private final ObjectMapper objectMapper;
    private final RagChunkProperties properties;

    public PersistenceMapper(ObjectProvider<ObjectMapper> objectMapper, RagChunkProperties properties) {
        this.objectMapper = objectMapper.getIfAvailable(ObjectMapper::new);
        this.properties = properties;
    }

    public KnowledgeBase toDomain(KnowledgeBaseEntity entity) {
        var kb = new KnowledgeBase();
        kb.setId(entity.getId());
        kb.setName(entity.getName());
        kb.setDescription(entity.getDescription());
        kb.setStatus(entity.getStatus());
        kb.setConfig(readConfig(entity.getConfigJson()));
        kb.setCreatedAt(entity.getCreatedAt());
        return kb;
    }

    public KnowledgeBaseEntity toEntity(KnowledgeBase kb, KnowledgeBaseEntity existing) {
        var entity = existing != null ? existing : new KnowledgeBaseEntity();
        Instant now = Instant.now();
        if (entity.getId() == null) {
            entity.setId(kb.getId());
            entity.setCreatedAt(kb.getCreatedAt() != null ? kb.getCreatedAt() : now);
        }
        entity.setName(kb.getName());
        entity.setDescription(kb.getDescription());
        entity.setStatus(kb.getStatus());
        entity.setConfigJson(writeConfig(kb.getConfig()));
        entity.setEmbeddingModel(kb.getConfig().embedding().model());
        entity.setEmbeddingDimensions(properties.getEmbedding().getDimensions());
        entity.setUpdatedAt(now);
        return entity;
    }

    public DocumentRecord toDomain(DocumentEntity entity) {
        var doc = new DocumentRecord();
        doc.setId(entity.getId());
        doc.setKbId(entity.getKbId());
        doc.setFileName(entity.getFileName());
        doc.setStatus(entity.getStatus());
        doc.setChunkCount(entity.getChunkCount());
        doc.setProfile(entity.getProfile());
        doc.setQualityScore(entity.getQualityScore());
        doc.setAiTriggered(entity.isAiTriggered());
        doc.setAiTriggerId(entity.getAiTriggerId());
        doc.setAiFallback(entity.isAiFallback());
        doc.setErrorMessage(entity.getErrorMessage());
        doc.setCreatedAt(entity.getCreatedAt());
        doc.setUpdatedAt(entity.getUpdatedAt());
        doc.setBatchId(entity.getBatchId());
        doc.setProcessStage(entity.getProcessStage());
        doc.setProgressPercent(entity.getProgressPercent());
        doc.setFileSize(entity.getFileSize());
        doc.setStorageKey(entity.getStorageKey());
        doc.setStorageUrl(entity.getStorageUrl());
        doc.setContentHash(entity.getContentHash());
        doc.setSourceType(entity.getSourceType());
        doc.setRetrainVersion(entity.getRetrainVersion());
        return doc;
    }

    public DocumentEntity toEntity(DocumentRecord doc, DocumentEntity existing) {
        var entity = existing != null ? existing : new DocumentEntity();
        Instant now = Instant.now();
        if (entity.getId() == null) {
            entity.setId(doc.getId());
            entity.setCreatedAt(doc.getCreatedAt() != null ? doc.getCreatedAt() : now);
        }
        entity.setKbId(doc.getKbId());
        entity.setFileName(doc.getFileName());
        entity.setStatus(doc.getStatus());
        entity.setChunkCount(doc.getChunkCount());
        entity.setProfile(doc.getProfile());
        entity.setQualityScore(doc.getQualityScore());
        entity.setAiTriggered(doc.isAiTriggered());
        entity.setAiTriggerId(doc.getAiTriggerId());
        entity.setAiFallback(doc.isAiFallback());
        entity.setErrorMessage(doc.getErrorMessage());
        entity.setBatchId(doc.getBatchId());
        entity.setProcessStage(doc.getProcessStage());
        entity.setProgressPercent(doc.getProgressPercent());
        entity.setFileSize(doc.getFileSize());
        entity.setStorageKey(doc.getStorageKey());
        entity.setStorageUrl(doc.getStorageUrl());
        entity.setContentHash(doc.getContentHash());
        entity.setSourceType(doc.getSourceType());
        entity.setRetrainVersion(doc.getRetrainVersion());
        entity.setUpdatedAt(now);
        return entity;
    }

    private KnowledgeBaseConfig readConfig(String json) {
        try {
            return objectMapper.readValue(json, KnowledgeBaseConfig.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("invalid config_json in database: " + e.getMessage());
        }
    }

    private String writeConfig(KnowledgeBaseConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("cannot serialize knowledge base config");
        }
    }
}
