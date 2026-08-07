package com.xtsh.ragchunk.objectstorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.util.ContentHashUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 默认实现：流式写入本地目录，URL 按配置的 OSS 默认 endpoint 拼接。
 */
@RequiredArgsConstructor
@Slf4j
@Service
@ConditionalOnProperty(name = "ragchunk.oss.provider", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorageService implements ObjectStorageService {

    private final RagChunkProperties properties;


    @Override
    public ArchiveResult putStream(String kbId, String docId, String fileName, InputStream input, long knownSize) {
        String key = kbId + "/" + docId + "/" + sanitize(fileName);
        Path target = resolvePath(key);
        try {
            Files.createDirectories(target.getParent());
            ContentHashUtil.StreamCopyResult copied;
            try (OutputStream out = Files.newOutputStream(target)) {
                copied = ContentHashUtil.copyWithSha256(input, out);
            }
            if (copied.bytesWritten() == 0) {
                throw new IllegalArgumentException("empty file content");
            }
            String url = publicUrl(key);
            log.info("[文档上传] 流式归档 local key={}, size={}B, url={}", key, copied.bytesWritten(), url);
            var stored = new StoredObject(key, url, copied.bytesWritten());
            return new ArchiveResult(stored, copied.sha256Hex());
        } catch (IOException e) {
            deleteQuietly(target);
            throw new RuntimeException("failed to archive original file: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream openStream(String storageKey) {
        try {
            return Files.newInputStream(resolvePath(storageKey));
        } catch (IOException e) {
            throw new RuntimeException("failed to open archived file: " + storageKey, e);
        }
    }

    @Override
    public byte[] get(String storageKey) {
        try {
            return Files.readAllBytes(resolvePath(storageKey));
        } catch (IOException e) {
            throw new RuntimeException("failed to read archived file: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        deleteQuietly(resolvePath(storageKey));
    }

    private Path resolvePath(String key) {
        Path root = Path.of(properties.getOss().getLocalRoot()).toAbsolutePath().normalize();
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("invalid storage path");
        }
        return target;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort
        }
    }

    private String publicUrl(String key) {
        String base = properties.getOss().getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = properties.getOss().getEndpoint() + "/" + properties.getOss().getBucket();
        }
        return base.endsWith("/") ? base + key : base + "/" + key;
    }

    private static String sanitize(String name) {
        return name == null ? "file.bin" : name.replace("\\", "_").replace("..", "_");
    }
}
