package com.xtsh.ragchunk.config;

import com.xtsh.ragchunk.integration.dashscope.DashScopeHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动时打印 LLM 是否可用，便于排查「LLM未配置」。
 */
@Component
public class LlmStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LlmStartupLogger.class);

    private final Environment environment;
    private final RagChunkProperties properties;
    private final DashScopeHttpClient dashScope;

    public LlmStartupLogger(Environment environment, RagChunkProperties properties,
                            DashScopeHttpClient dashScope) {
        this.environment = environment;
        this.properties = properties;
        this.dashScope = dashScope;
    }

    @Override
    public void run(ApplicationArguments args) {
        var ds = properties.getDashscope();
        boolean configured = dashScope.isConfigured();
        log.info("[启动] activeProfiles={}, llmProvider={}, llmConfigured={}, baseUrl={}, "
                        + "chunkModel={}, embeddingRemote={}",
                String.join(",", environment.getActiveProfiles()),
                ds.getProvider(), configured, ds.getBaseUrl(),
                properties.getAi().getChunkModel(), properties.getEmbedding().isRemoteEnabled());
        if (!configured) {
            if (ds.isOllama()) {
                log.warn("[启动] LLM 未配置：provider=ollama 但 base-url 为空，请设置 OLLAMA_BASE_URL");
            } else {
                log.warn("[启动] LLM 未配置：provider=dashscope 且未设置 DASHSCOPE_API_KEY；"
                        + "或改用 profile/local 中的 Ollama（provider=ollama）");
            }
        } else if (ds.isOllama()) {
            log.info("[启动] Ollama 地址={} ，若 AI 请求 ConnectException 请确认该地址可访问（如 curl {}/api/tags）",
                    ds.getBaseUrl(), ds.getBaseUrl().replace("/v1", ""));
        }
    }
}
