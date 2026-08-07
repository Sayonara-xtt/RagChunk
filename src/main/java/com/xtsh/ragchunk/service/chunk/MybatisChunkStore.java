package com.xtsh.ragchunk.service.chunk;

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xtsh.ragchunk.dto.chunk.StoredChunk;
import com.xtsh.ragchunk.util.PersistenceMapper;
import com.xtsh.ragchunk.entity.ChunkEntity;
import com.xtsh.ragchunk.mapper.ChunkMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
public class MybatisChunkStore implements ChunkStore {

    private final ChunkMapper chunkMapper;
    private final PersistenceMapper mapper;

    @Override
    public List<StoredChunk> findByKbId(String kbId) {
        return chunkMapper.selectList(Wrappers.lambdaQuery(ChunkEntity.class)
                        .eq(ChunkEntity::getKbId, kbId)
                        .orderByAsc(ChunkEntity::getDocId)
                        .orderByAsc(ChunkEntity::getChunkIndex))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<StoredChunk> findByDocId(String kbId, String docId) {
        return chunkMapper.selectList(Wrappers.lambdaQuery(ChunkEntity.class)
                        .eq(ChunkEntity::getKbId, kbId)
                        .eq(ChunkEntity::getDocId, docId)
                        .orderByAsc(ChunkEntity::getChunkIndex))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<StoredChunk> findById(String kbId, String chunkId) {
        return Optional.ofNullable(chunkMapper.selectOne(Wrappers.lambdaQuery(ChunkEntity.class)
                        .eq(ChunkEntity::getKbId, kbId)
                        .eq(ChunkEntity::getId, chunkId)))
                .map(mapper::toDomain);
    }
}
