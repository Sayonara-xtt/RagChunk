package com.xtsh.ragchunk.web;

import com.xtsh.ragchunk.config.RagChunkProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "运行配置", description = "查看当前激活的 Spring Profile 与 LLM 配置（排障用）")
@RestController
@RequestMapping("/api/v1/runtime")
public class RuntimeConfigController {

    private final Environment environment;
    private final RagChunkProperties properties;

    public RuntimeConfigController(Environment environment, RagChunkProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @Operation(summary = "运行时配置", description = "含 activeProfiles、ollama 是否生效（provider=ollama）")
    @GetMapping
    public Map<String, Object> runtime() {
        var ds = properties.getDashscope();
        var map = new LinkedHashMap<String, Object>();
        map.put("activeProfiles", environment.getActiveProfiles());
        map.put("defaultProfiles", environment.getDefaultProfiles());
        map.put("ollamaProfileActive", containsProfile("ollama"));
        map.put("llmProvider", ds.getProvider());
        map.put("llmConfigured", ds.isLlmConfigured());
        map.put("llmBaseUrl", ds.getBaseUrl());
        map.put("llmApiKeySet", ds.getApiKey() != null && !ds.getApiKey().isBlank());
        map.put("llmConfigHint", llmConfigHint(ds));
        map.put("chatModel", properties.getChat().getModel());
        map.put("chunkModel", properties.getAi().getChunkModel());
        map.put("embeddingRemoteEnabled", properties.getEmbedding().isRemoteEnabled());
        map.put("embeddingModel", properties.getEmbedding().getModel());
        return map;
    }

    private boolean containsProfile(String name) {
        for (String p : environment.getActiveProfiles()) {
            if (name.equalsIgnoreCase(p)) return true;
        }
        return false;
    }

    private static String llmConfigHint(RagChunkProperties.Dashscope ds) {
        if (ds.isLlmConfigured()) {
            return "ok";
        }
        if (ds.isOllama()) {
            return "ollama: set OLLAMA_BASE_URL (e.g. http://localhost:11434/v1) and restart";
        }
        return "dashscope: set DASHSCOPE_API_KEY or use profiles local,ollama / application-local ollama";
    }
}
