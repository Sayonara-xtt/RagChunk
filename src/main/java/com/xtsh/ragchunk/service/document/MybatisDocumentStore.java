package com.xtsh.ragchunk.service.document;

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xtsh.ragchunk.dto.document.DocumentRecord;
import com.xtsh.ragchunk.util.PersistenceMapper;
import com.xtsh.ragchunk.entity.DocumentEntity;
import com.xtsh.ragchunk.mapper.DocumentMapper;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseStore;
import com.xtsh.ragchunk.exception.NotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
public class MybatisDocumentStore implements DocumentStore {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseStore knowledgeBaseStore;
    private final PersistenceMapper mapper;

    @Override
    @Transactional
    public DocumentRecord save(DocumentRecord doc) {
        knowledgeBaseStore.findById(doc.getKbId())
                .orElseThrow(() -> new NotFoundException("knowledge base not found: " + doc.getKbId()));
        DocumentEntity existing = documentMapper.selectById(doc.getId());
        DocumentEntity entity = mapper.toEntity(doc, existing);
        if (existing == null) {
            documentMapper.insert(entity);
        } else {
            documentMapper.updateById(entity);
        }
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<DocumentRecord> findById(String id) {
        return Optional.ofNullable(documentMapper.selectById(id)).map(mapper::toDomain);
    }

    @Override
    public List<DocumentRecord> findByKbId(String kbId) {
        var wrapper = new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getKbId, kbId)
                .orderByDesc(DocumentEntity::getCreatedAt);
        return documentMapper.selectList(wrapper).stream().map(mapper::toDomain).toList();
    }
}
