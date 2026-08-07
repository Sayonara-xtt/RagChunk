package com.xtsh.ragchunk.service.knowledge;

import lombok.RequiredArgsConstructor;
import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.dto.knowledge.KnowledgeBaseConfig;
import com.xtsh.ragchunk.dto.knowledge.QaConfig;
import org.springframework.stereotype.Component;

/**
 * 为历史知识库 config_json 补全缺失的 {@link QaConfig}（旧库无 qa 字段时）。
 */
@RequiredArgsConstructor
@Component
public class KnowledgeBaseConfigNormalizer {

    private final RagChunkProperties defaults;


    public KnowledgeBaseConfig normalize(KnowledgeBaseConfig config) {
        if (config == null) {
            return null;
        }
        if (config.qa() != null) {
            return config;
        }
        return new KnowledgeBaseConfig(
                config.chunking(),
                config.rule(),
                config.quality(),
                config.ai(),
                config.embedding(),
                config.retrieval(),
                QaConfig.fromDefaults(defaults)
        );
    }
}
