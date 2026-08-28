package com.xtsh.ragchunk.service.knowledge;

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xtsh.ragchunk.dto.knowledge.KnowledgeBase;
import com.xtsh.ragchunk.util.PersistenceMapper;
import com.xtsh.ragchunk.entity.KnowledgeBaseEntity;
import com.xtsh.ragchunk.mapper.KnowledgeBaseMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
public class MybatisKnowledgeBaseStore implements KnowledgeBaseStore {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final PersistenceMapper mapper;


    @Override
    @Transactional
    public KnowledgeBase save(KnowledgeBase kb) {
        KnowledgeBaseEntity existing = knowledgeBaseMapper.selectById(kb.getId());
        KnowledgeBaseEntity entity = mapper.toEntity(kb, existing);
        if (existing == null) {
            knowledgeBaseMapper.insert(entity);
        } else {
            knowledgeBaseMapper.updateById(entity);
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
