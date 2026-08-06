package com.xtsh.ragchunk.knowledge;

import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.dto.knowledge.CreateKnowledgeBaseRequest;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseConfigMerger;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseConfigNormalizer;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseRequestValidator;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.service.knowledge.InMemoryKnowledgeBaseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeBaseServiceTest {

    private KnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        var props = new RagChunkProperties();
        service = new KnowledgeBaseService(
                props,
                new KnowledgeBaseConfigMerger(),
                new KnowledgeBaseRequestValidator(),
                new InMemoryKnowledgeBaseStore(),
                new KnowledgeBaseConfigNormalizer(props)
        );
    }

    @Test
    void listAllAndGetById() {
        var req = new CreateKnowledgeBaseRequest();
        req.setName("库A");
        var a = service.create(req);
        req.setName("库B");
        var b = service.create(req);

        assertEquals(2, service.listAll().size());
        assertEquals(a.getId(), service.getById(a.getId()).getId());
        assertEquals("库B", service.getById(b.getId()).getName());
    }

    @Test
    void createWithPartialParams() {
        var req = new CreateKnowledgeBaseRequest();
        req.setName("测试库");
        var c = new CreateKnowledgeBaseRequest.ChunkingPayload();
        c.setAiMode("never");
        req.setChunking(c);
        var resp = service.create(req);
        assertTrue(resp.getId().startsWith("kb_"));
        assertEquals("never", resp.getConfig().chunking().aiMode());
    }
}
