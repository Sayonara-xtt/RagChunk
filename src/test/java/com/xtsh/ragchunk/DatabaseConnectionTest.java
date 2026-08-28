package com.xtsh.ragchunk;

import com.xtsh.ragchunk.entity.DocumentEntity;
import com.xtsh.ragchunk.entity.KnowledgeBaseEntity;
import com.xtsh.ragchunk.mapper.DocumentMapper;
import com.xtsh.ragchunk.mapper.KnowledgeBaseMapper;
import com.xtsh.ragchunk.vector.VectorRecord;
import com.xtsh.ragchunk.vector.VectorStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** 由 {@code scripts/test-db-connection.ps1} 触发，设置 {@code RUN_DB_TEST=1} */
@EnabledIfEnvironmentVariable(named = "RUN_DB_TEST", matches = "1")
@SpringBootTest
@ActiveProfiles("local")
@Import(PostgresPersistenceTestConfig.class)
@Tag("integration")
class DatabaseConnectionTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    DocumentMapper documentMapper;

    @Autowired
    VectorStore vectorStore;

    @Test
    void connectionAndSchema() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertTrue(conn.isValid(5));
        }

        String db = jdbc.queryForObject("SELECT current_database()", String.class);
        String user = jdbc.queryForObject("SELECT current_user", String.class);
        Boolean vectorExt = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')", Boolean.class);
        Integer kbCount = jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_base", Integer.class);

        System.out.println("database=" + db + ", user=" + user + ", pgvector=" + vectorExt + ", knowledge_base rows=" + kbCount);
        assertNotNull(db);
        assertTrue(vectorExt, "pgvector extension should be installed");
    }

    @Test
    @Transactional
    void jsonbAndPgVectorMapperBoundaries() throws Exception {
        Instant now = Instant.now();
        var knowledgeBase = new KnowledgeBaseEntity();
        knowledgeBase.setId("kb_sql_boundary_test");
        knowledgeBase.setName("SQL boundary test");
        knowledgeBase.setStatus("READY");
        knowledgeBase.setConfigJson("{\"strategy\":\"hybrid\"}");
        knowledgeBase.setEmbeddingModel("test");
        knowledgeBase.setEmbeddingDimensions(1024);
        knowledgeBase.setCreatedAt(now);
        knowledgeBase.setUpdatedAt(now);
        assertEquals(1, knowledgeBaseMapper.insert(knowledgeBase));
        String storedConfig = knowledgeBaseMapper.selectById(knowledgeBase.getId()).getConfigJson();
        assertEquals("hybrid", new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(storedConfig).path("strategy").asText());
        knowledgeBase.setConfigJson("{\"strategy\":\"semantic\"}");
        assertEquals(1, knowledgeBaseMapper.updateById(knowledgeBase));
        storedConfig = knowledgeBaseMapper.selectById(knowledgeBase.getId()).getConfigJson();
        assertEquals("semantic", new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(storedConfig).path("strategy").asText());

        var document = new DocumentEntity();
        document.setId("doc_sql_boundary_test");
        document.setKbId(knowledgeBase.getId());
        document.setFileName("sql-boundary-test.txt");
        document.setStatus("SUCCESS");
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        assertEquals(1, documentMapper.insert(document));

        float[] embedding = new float[1024];
        embedding[0] = 1.0f;
        vectorStore.upsert(new VectorRecord(
                knowledgeBase.getId(), document.getId(), "chunk_sql_boundary_test", 0,
                "mapper xml text", "RULE", embedding
        ));
        vectorStore.upsert(new VectorRecord(
                knowledgeBase.getId(), document.getId(), "chunk_sql_boundary_test", 0,
                "mapper xml updated", "RULE", embedding
        ));

        var result = vectorStore.search(knowledgeBase.getId(), embedding, 5, 0.99);
        assertEquals(1, result.size());
        assertEquals("chunk_sql_boundary_test", result.get(0).record().chunkId());
        assertEquals("mapper xml updated", result.get(0).record().text());
        assertEquals(1.0, result.get(0).score(), 0.0001);

        vectorStore.deleteByDocId(document.getId());
        assertEquals(0, vectorStore.search(knowledgeBase.getId(), embedding, 5, 0.99).size());
    }
}
