package com.xtsh.ragchunk.integration.dashscope;


import com.xtsh.ragchunk.config.RagChunkProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一 LLM / Embedding HTTP 客户端。切片与问答均走 OpenAI 兼容 {@code /v1/chat/completions}。
 * <p>provider=ollama 时请求本地 Ollama（无需 Authorization）；否则走 DashScope。
 */
@Component
public class DashScopeHttpClient {

    private final RagChunkProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    public DashScopeHttpClient(RagChunkProperties properties) {
        this.properties = properties;
    }

    /** Ollama：baseUrl 非空即可；DashScope：需 DASHSCOPE_API_KEY */
    public boolean isConfigured() {
        return properties.getDashscope().isLlmConfigured();
    }

    /**
     * 语义切片与 RAG 问答共用。模型名来自知识库 config.ai.chunkModel 或 ragchunk.chat.model。
     */
    public String chat(String model, String systemPrompt, String userContent) throws Exception {
        return postChat(model, systemPrompt, userContent, false);
    }

    /**
     * 语义切片专用：降低随机性并请求 JSON 输出（Ollama {@code format=json} / DashScope response_format）。
     */
    public String chatJson(String model, String systemPrompt, String userContent) throws Exception {
        return postChat(model, systemPrompt, userContent, true);
    }

    private String postChat(String model, String systemPrompt, String userContent, boolean jsonMode) throws Exception {
        var messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        );
        var body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("messages", messages);
        if (jsonMode) {
            body.put("temperature", 0);
            if (properties.getDashscope().isOllama()) {
                body.put("format", "json");
            } else {
                body.put("response_format", Map.of("type", "json_object"));
            }
        }
        var bodyJson = mapper.writeValueAsString(body);
        var ds = properties.getDashscope();
        var req = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBase(ds.getBaseUrl()) + "/chat/completions"))
                .header("Content-Type", "application/json");
        if (!ds.isOllama() && ds.getApiKey() != null && !ds.getApiKey().isBlank()) {
            req = req.header("Authorization", "Bearer " + ds.getApiKey());
        }
        req = req.POST(HttpRequest.BodyPublishers.ofString(bodyJson)).timeout(Duration.ofMinutes(5));
        String chatUrl = normalizeBase(ds.getBaseUrl()) + "/chat/completions";
        HttpResponse<String> resp;
        try {
            resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException(providerLabel() + " chat 连接失败: " + chatUrl + " — "
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e);
        }
        if (resp.statusCode() >= 400) {
            throw new IllegalStateException(providerLabel() + " chat error: " + resp.statusCode() + " " + resp.body());
        }
        JsonNode root = mapper.readTree(resp.body());
        return root.path("choices").path(0).path("message").path("content").asText();
    }

    public float[] embed(String model, String text, int dimensions) throws Exception {
        var ds = properties.getDashscope();
        if (ds.isOllama()) {
            return embedOllama(model, text, dimensions);
        }
        return embedDashScope(model, text, dimensions);
    }

    private float[] embedDashScope(String model, String text, int dimensions) throws Exception {
        var input = Map.of("model", model, "input", Map.of("texts", List.of(text)), "parameters", Map.of("dimension", dimensions));
        var body = mapper.writeValueAsString(input);
        var ds = properties.getDashscope();
        var req = HttpRequest.newBuilder()
                .uri(URI.create(ds.getEmbeddingUrl()))
                .header("Authorization", "Bearer " + ds.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofMinutes(1))
                .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IllegalStateException("dashscope embed error: " + resp.statusCode() + " " + resp.body());
        }
        JsonNode arr = mapper.readTree(resp.body()).path("output").path("embeddings").path(0).path("embedding");
        return toVector(arr, dimensions);
    }

    private float[] embedOllama(String model, String text, int dimensions) throws Exception {
        var body = mapper.writeValueAsString(Map.of("model", model, "input", text));
        var url = normalizeBase(properties.getDashscope().getBaseUrl()) + "/embeddings";
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofMinutes(1))
                .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IllegalStateException("ollama embed error: " + resp.statusCode() + " " + resp.body());
        }
        JsonNode arr = mapper.readTree(resp.body()).path("data").path(0).path("embedding");
        return toVector(arr, dimensions);
    }

    private static float[] toVector(JsonNode arr, int expectedDim) {
        float[] vec = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            vec[i] = (float) arr.get(i).asDouble();
        }
        if (expectedDim > 0 && vec.length != expectedDim) {
            throw new IllegalStateException("embedding dimension mismatch: got " + vec.length + ", expected " + expectedDim);
        }
        return vec;
    }

    private static String normalizeBase(String baseUrl) {
        if (baseUrl == null) return "";
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String providerLabel() {
        return properties.getDashscope().isOllama() ? "ollama" : "dashscope";
    }
}
