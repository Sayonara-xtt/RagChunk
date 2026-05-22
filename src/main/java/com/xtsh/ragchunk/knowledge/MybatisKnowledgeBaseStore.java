package com.xtsh.ragchunk.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBase;
import com.xtsh.ragchunk.persistence.PersistenceMapper;
import com.xtsh.ragchunk.persistence.entity.KnowledgeBaseEntity;
import com.xtsh.ragchunk.persistence.mapper.KnowledgeBaseMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
public class MybatisKnowledgeBaseStore implements KnowledgeBaseStore {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final PersistenceMapper mapper;

    public MybatisKnowledgeBaseStore(KnowledgeBaseMapper knowledgeBaseMapper, PersistenceMapper mapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public KnowledgeBase save(KnowledgeBase kb) {
        KnowledgeBaseEntity existing = knowledgeBaseMapper.selectById(kb.getId());
        KnowledgeBaseEntity entity = mapper.toEntity(kb, existing);
        if (existing == null) {
            knowledgeBaseMapper.insertRow(entity);
        } else {
            knowledgeBaseMapper.updateRow(entity);
        }
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<KnowledgeBase> findById(String id) {
        return Optional.ofNullable(knowledgeBaseMapper.selectById(id)).map(mapper::toDomain);
    }

    @Override
    public List<KnowledgeBase> findAll() {
        var wrapper = new LambdaQueryWrapper<KnowledgeBaseEntity>()
                .orderByDesc(KnowledgeBaseEntity::getCreatedAt);
        return knowledgeBaseMapper.selectList(wrapper).stream().map(mapper::toDomain).toList();
    }
}
