package com.xtsh.ragchunk.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xtsh.ragchunk.document.model.DocumentRecord;
import com.xtsh.ragchunk.persistence.PersistenceMapper;
import com.xtsh.ragchunk.persistence.entity.DocumentEntity;
import com.xtsh.ragchunk.persistence.mapper.DocumentMapper;
import com.xtsh.ragchunk.persistence.mapper.KnowledgeBaseMapper;
import com.xtsh.ragchunk.web.NotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
public class MybatisDocumentStore implements DocumentStore {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final PersistenceMapper mapper;

    public MybatisDocumentStore(DocumentMapper documentMapper, KnowledgeBaseMapper knowledgeBaseMapper,
                                PersistenceMapper mapper) {
        this.documentMapper = documentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public DocumentRecord save(DocumentRecord doc) {
        if (knowledgeBaseMapper.selectById(doc.getKbId()) == null) {
            throw new NotFoundException("knowledge base not found: " + doc.getKbId());
        }
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
