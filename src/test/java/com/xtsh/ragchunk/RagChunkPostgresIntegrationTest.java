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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 需本地 Docker 可用时运行：{@code set RUN_PG_INTEGRATION=1 && mvn test -Dtest=RagChunkPostgresIntegrationTest}
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresPersistenceTestConfig.class)
@EnabledIfEnvironmentVariable(named = "RUN_PG_INTEGRATION", matches = "1")
@Tag("integration")
class RagChunkPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("ragchunk")
            .withUsername("ragchunk")
            .withPassword("ragchunk");

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private VectorStore vectorStore;

    @Test
    void contextLoadsWithPostgres() {
    }

    @Test
    @Transactional
    void persistsJsonbAndSearchesPgVectorThroughMapperXml() throws Exception {
        Instant now = Instant.now();
        var knowledgeBase = new KnowledgeBaseEntity();
        knowledgeBase.setId("kb_mapper_test");
        knowledgeBase.setName("mapper test");
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
        document.setId("doc_mapper_test");
        document.setKbId(knowledgeBase.getId());
        document.setFileName("mapper-test.txt");
        document.setStatus("SUCCESS");
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        assertEquals(1, documentMapper.insert(document));

        float[] embedding = new float[1024];
        embedding[0] = 1.0f;
        vectorStore.upsert(new VectorRecord(
                knowledgeBase.getId(), document.getId(), "chunk_mapper_test", 0,
                "mapper xml text", "RULE", embedding
        ));
        vectorStore.upsert(new VectorRecord(
                knowledgeBase.getId(), document.getId(), "chunk_mapper_test", 0,
                "mapper xml updated", "RULE", embedding
        ));

        var result = vectorStore.search(knowledgeBase.getId(), embedding, 5, 0.99);
        assertEquals(1, result.size());
        assertEquals("chunk_mapper_test", result.get(0).record().chunkId());
        assertEquals("mapper xml updated", result.get(0).record().text());
        assertEquals(1.0, result.get(0).score(), 0.0001);

        vectorStore.deleteByDocId(document.getId());
        assertEquals(0, vectorStore.search(knowledgeBase.getId(), embedding, 5, 0.99).size());
    }
}
