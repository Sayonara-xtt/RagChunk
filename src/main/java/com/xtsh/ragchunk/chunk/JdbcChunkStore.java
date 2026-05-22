package com.xtsh.ragchunk.chunk;

import com.xtsh.ragchunk.chunk.model.StoredChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
public class JdbcChunkStore implements ChunkStore {

    private static final String SELECT_COLS = """
            id, kb_id, doc_id, chunk_index, text_content, source, created_at
            """;

    private final JdbcTemplate jdbc;
    private final RowMapper<StoredChunk> rowMapper = (rs, rowNum) -> {
        var c = new StoredChunk();
        c.setId(rs.getString("id"));
        c.setKbId(rs.getString("kb_id"));
        c.setDocId(rs.getString("doc_id"));
        c.setChunkIndex(rs.getInt("chunk_index"));
        c.setTextContent(rs.getString("text_content"));
        c.setSource(rs.getString("source"));
        Timestamp ts = rs.getTimestamp("created_at");
        c.setCreatedAt(ts != null ? ts.toInstant() : null);
        return c;
    };

    public JdbcChunkStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<StoredChunk> findByKbId(String kbId) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM chunk WHERE kb_id = ? ORDER BY doc_id, chunk_index",
                rowMapper, kbId);
    }

    @Override
    public List<StoredChunk> findByDocId(String kbId, String docId) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM chunk WHERE kb_id = ? AND doc_id = ? ORDER BY chunk_index",
                rowMapper, kbId, docId);
    }

    @Override
    public Optional<StoredChunk> findById(String kbId, String chunkId) {
        var list = jdbc.query(
                "SELECT " + SELECT_COLS + " FROM chunk WHERE kb_id = ? AND id = ?",
                rowMapper, kbId, chunkId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
