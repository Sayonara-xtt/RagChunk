package com.xtsh.ragchunk.vector;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * 本机 PostgreSQL 持久化（无需 pgvector），向量存 real[]，检索在应用内算余弦相似度。
 */
@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
@ConditionalOnProperty(name = "ragchunk.storage.vector-store", havingValue = "array")
public class JdbcArrayVectorStore implements VectorStore {

    private final JdbcTemplate jdbc;

    public JdbcArrayVectorStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(VectorRecord record) {
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO chunk (id, kb_id, doc_id, chunk_index, text_content, source, embedding, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        text_content = EXCLUDED.text_content,
                        source = EXCLUDED.source,
                        embedding = EXCLUDED.embedding
                    """);
            ps.setString(1, record.chunkId());
            ps.setString(2, record.kbId());
            ps.setString(3, record.docId());
            ps.setInt(4, record.chunkIndex());
            ps.setString(5, record.text());
            ps.setString(6, record.source());
            ps.setArray(7, toSqlArray(connection, record.embedding()));
            ps.setTimestamp(8, Timestamp.from(Instant.now()));
            return ps;
        });
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
        return jdbc.query("""
                SELECT id, kb_id, doc_id, chunk_index, text_content, source, embedding
                FROM chunk WHERE kb_id = ?
                """, (rs, rowNum) -> {
            float[] emb = readEmbedding(rs.getArray("embedding"));
            var record = new VectorRecord(
                    rs.getString("kb_id"),
                    rs.getString("doc_id"),
                    rs.getString("id"),
                    rs.getInt("chunk_index"),
                    rs.getString("text_content"),
                    rs.getString("source"),
                    emb
            );
            return new ScoredChunk(record, cosine(query, emb));
        }, kbId).stream()
                .filter(s -> s.score() >= minScore)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    private static Array toSqlArray(Connection conn, float[] vec) throws SQLException {
        Float[] boxed = new Float[vec.length];
        for (int i = 0; i < vec.length; i++) boxed[i] = vec[i];
        return conn.createArrayOf("real", boxed);
    }

    private static float[] readEmbedding(Array sqlArray) throws SQLException {
        if (sqlArray == null) return new float[0];
        Object arr = sqlArray.getArray();
        if (arr instanceof Float[] floats) {
            float[] out = new float[floats.length];
            for (int i = 0; i < floats.length; i++) out[i] = floats[i] != null ? floats[i] : 0f;
            return out;
        }
        if (arr instanceof Double[] doubles) {
            float[] out = new float[doubles.length];
            for (int i = 0; i < doubles.length; i++) out[i] = doubles[i] != null ? doubles[i].floatValue() : 0f;
            return out;
        }
        return new float[0];
    }

    private static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
