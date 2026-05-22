package com.xtsh.ragchunk.embedding;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** 无 API Key 时用于本地开发的确定性向量 */
@Component
public class LocalHashEmbeddingService {

    public float[] embed(String text, int dimensions) {
        float[] vec = new float[dimensions];
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            vec[i % dimensions] += bytes[i];
        }
        double norm = 0;
        for (float v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vec.length; i++) vec[i] /= (float) norm;
        }
        return vec;
    }
}
