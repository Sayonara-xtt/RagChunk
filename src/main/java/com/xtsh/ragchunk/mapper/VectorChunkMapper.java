package com.xtsh.ragchunk.mapper;

import com.xtsh.ragchunk.vector.VectorRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VectorChunkMapper {

    int upsertPgVector(@Param("record") VectorRecord record);

    int upsertArray(@Param("record") VectorRecord record);

    int deleteByDocId(@Param("docId") String docId);

    int deleteByKbId(@Param("kbId") String kbId);

    List<VectorChunkRow> searchPgVector(
            @Param("kbId") String kbId,
            @Param("query") float[] query,
            @Param("topK") int topK,
            @Param("minScore") double minScore
    );

    List<VectorChunkRow> findArrayByKbId(@Param("kbId") String kbId);
}
