package com.xtsh.ragchunk.knowledge;

import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.knowledge.dto.CreateKnowledgeBaseRequest;
import com.xtsh.ragchunk.knowledge.dto.KnowledgeBaseResponse;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBase;
import com.xtsh.ragchunk.web.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseService {

    private final RagChunkProperties defaults;
    private final KnowledgeBaseConfigMerger merger;
    private final KnowledgeBaseRequestValidator validator;
    private final KnowledgeBaseStore store;
    private final KnowledgeBaseConfigNormalizer configNormalizer;

    public KnowledgeBaseService(RagChunkProperties defaults, KnowledgeBaseConfigMerger merger,
                                KnowledgeBaseRequestValidator validator, KnowledgeBaseStore store,
                                KnowledgeBaseConfigNormalizer configNormalizer) {
        this.defaults = defaults;
        this.merger = merger;
        this.validator = validator;
        this.store = store;
        this.configNormalizer = configNormalizer;
    }

    public KnowledgeBaseResponse create(CreateKnowledgeBaseRequest request) {
        validator.validateCreate(request);
        var config = merger.merge(defaults, request);
        validator.validateConfig(config);
        var kb = new KnowledgeBase();
        kb.setId("kb_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        kb.setName(request.getName().trim());
        kb.setDescription(request.getDescription());
        kb.setStatus("READY");
        kb.setConfig(config);
        kb.setCreatedAt(Instant.now());
        store.save(kb);
        return toResponse(kb);
    }

    public KnowledgeBase require(String id) {
        var kb = store.findById(id).orElseThrow(() -> new NotFoundException("knowledge base not found: " + id));
        kb.setConfig(configNormalizer.normalize(kb.getConfig()));
        return kb;
    }

    public KnowledgeBaseResponse getById(String id) {
        return toResponse(require(id));
    }

    public List<KnowledgeBaseResponse> listAll() {
        return store.findAll().stream().map(this::toResponse).toList();
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase kb) {
        var r = new KnowledgeBaseResponse();
        r.setId(kb.getId());
        r.setName(kb.getName());
        r.setDescription(kb.getDescription());
        r.setStatus(kb.getStatus());
        r.setConfig(kb.getConfig());
        r.setCreatedAt(kb.getCreatedAt());
        return r;
    }
}
