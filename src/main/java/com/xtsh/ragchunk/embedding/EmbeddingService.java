package com.xtsh.ragchunk.embedding;

import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.integration.dashscope.DashScopeHttpClient;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final RagChunkProperties properties;
    private final DashScopeHttpClient dashScope;
    private final LocalHashEmbeddingService localHash;

    public EmbeddingService(RagChunkProperties properties, DashScopeHttpClient dashScope,
                            LocalHashEmbeddingService localHash) {
        this.properties = properties;
        this.dashScope = dashScope;
        this.localHash = localHash;
    }

    public float[] embed(String text, KnowledgeBaseConfig.EmbeddingConfig config) throws Exception {
        int dim = properties.getEmbedding().getDimensions();
        if (properties.getEmbedding().isRemoteEnabled() && dashScope.isConfigured()) {
            return dashScope.embed(config.model(), text, dim);
        }
        return localHash.embed(text, dim);
    }
}
