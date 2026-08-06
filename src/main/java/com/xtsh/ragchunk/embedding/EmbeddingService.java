package com.xtsh.ragchunk.embedding;

import lombok.RequiredArgsConstructor;
import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.integration.dashscope.DashScopeHttpClient;
import com.xtsh.ragchunk.dto.knowledge.KnowledgeBaseConfig;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EmbeddingService {

    private final RagChunkProperties properties;
    private final DashScopeHttpClient dashScope;
    private final LocalHashEmbeddingService localHash;


    public float[] embed(String text, KnowledgeBaseConfig.EmbeddingConfig config) throws Exception {
        int dim = properties.getEmbedding().getDimensions();
        if (properties.getEmbedding().isRemoteEnabled() && dashScope.isConfigured()) {
            return dashScope.embed(config.model(), text, dim);
        }
        return localHash.embed(text, dim);
    }
}
