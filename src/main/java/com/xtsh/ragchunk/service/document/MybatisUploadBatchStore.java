package com.xtsh.ragchunk.service.document;

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xtsh.ragchunk.dto.document.UploadBatchRecord;
import com.xtsh.ragchunk.entity.UploadBatchEntity;
import com.xtsh.ragchunk.mapper.UploadBatchMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
public class MybatisUploadBatchStore implements UploadBatchStore {

    private final UploadBatchMapper mapper;


    @Override
    public UploadBatchRecord save(UploadBatchRecord batch) {
        UploadBatchEntity existing = mapper.selectById(batch.getId());
        UploadBatchEntity entity = toEntity(batch, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public Optional<UploadBatchRecord> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<UploadBatchRecord> findByKbId(String kbId, int limit) {
        var w = new LambdaQueryWrapper<UploadBatchEntity>()
                .eq(UploadBatchEntity::getKbId, kbId)
                .orderByDesc(UploadBatchEntity::getCreatedAt)
                .last("LIMIT " + Math.max(1, limit));
        return mapper.selectList(w).stream().map(this::toDomain).toList();
    }

    private UploadBatchEntity toEntity(UploadBatchRecord b, UploadBatchEntity existing) {
        var e = existing != null ? existing : new UploadBatchEntity();
        Instant now = Instant.now();
        if (e.getId() == null) {
            e.setId(b.getId());
            e.setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt() : now);
        }
        e.setKbId(b.getKbId());
        e.setSourceType(b.getSourceType());
        e.setStatus(b.getStatus());
        e.setTotalCount(b.getTotalCount());
        e.setQueuedCount(b.getQueuedCount());
        e.setProcessingCount(b.getProcessingCount());
        e.setSuccessCount(b.getSuccessCount());
        e.setFailedCount(b.getFailedCount());
        e.setSmartChunk(b.isSmartChunk());
        e.setErrorMessage(b.getErrorMessage());
        e.setUpdatedAt(now);
        return e;
    }

    private UploadBatchRecord toDomain(UploadBatchEntity e) {
        var b = new UploadBatchRecord();
        b.setId(e.getId());
        b.setKbId(e.getKbId());
        b.setSourceType(e.getSourceType());
        b.setStatus(e.getStatus());
        b.setTotalCount(e.getTotalCount());
        b.setQueuedCount(e.getQueuedCount());
        b.setProcessingCount(e.getProcessingCount());
        b.setSuccessCount(e.getSuccessCount());
        b.setFailedCount(e.getFailedCount());
        b.setSmartChunk(e.isSmartChunk());
        b.setErrorMessage(e.getErrorMessage());
        b.setCreatedAt(e.getCreatedAt());
        b.setUpdatedAt(e.getUpdatedAt());
        return b;
    }
}
