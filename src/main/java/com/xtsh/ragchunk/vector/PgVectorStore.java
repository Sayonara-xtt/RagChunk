package com.xtsh.ragchunk.vector;

import com.pgvector.PGvector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
@ConditionalOnProperty(name = "ragchunk.storage.vector-store", havingValue = "pgvector", matchIfMissing = true)
public class PgVectorStore implements VectorStore {

    private final JdbcTemplate jdbc;

    public PgVectorStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(VectorRecord record) {
        String sql = """
                INSERT INTO chunk (id, kb_id, doc_id, chunk_index, text_content, source, embedding, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    text_content = EXCLUDED.text_content,
                    source = EXCLUDED.source,
                    embedding = EXCLUDED.embedding
                """;
        jdbc.update(sql,
                record.chunkId(),
                record.kbId(),
                record.docId(),
                record.chunkIndex(),
                record.text(),
                record.source(),
                new PGvector(record.embedding()),
                Timestamp.from(Instant.now()));
    }

    @Override
    public void deleteByDocId(String docId) {
        jdbc.update("DELETE FROM chunk WHERE doc_id = ?", docId);
    }

    @Override
    public void deleteByKbId(String kbId) {
        jdbc.update("DELETE FROM chunk WHERE kb_id = ?", kbId);
    }

    @Override
    public List<ScoredChunk> search(String kbId, float[] query, int topK, double minScore) {
        String sql = """
                SELECT id, kb_id, doc_id, chunk_index, text_content, source,
                       1 - (embedding <=> ?::vector) AS score
                FROM chunk
                WHERE kb_id = ?
                  AND 1 - (embedding <=> ?::vector) >= ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;
        PGvector queryVec = new PGvector(query);
        return jdbc.query(sql, (rs, rowNum) -> {
            double score = rs.getDouble("score");
            var record = new VectorRecord(
                    rs.getString("kb_id"),
                    rs.getString("doc_id"),
                    rs.getString("id"),
                    rs.getInt("chunk_index"),
                    rs.getString("text_content"),
                    rs.getString("source"),
                    query
            );
            return new ScoredChunk(record, score);
        }, queryVec, kbId, queryVec, minScore, queryVec, topK);
    }
}
