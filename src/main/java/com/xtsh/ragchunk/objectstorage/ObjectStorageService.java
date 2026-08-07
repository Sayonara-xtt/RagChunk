package com.xtsh.ragchunk.objectstorage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 文档原件归档（本地目录或 S3 兼容 OSS）。
 */
public interface ObjectStorageService {

    /**
     * 流式上传原件（推荐）：边读边写，同时计算 SHA-256，避免整文件进堆。
     *
     * @param knownSize {@link InputStream} 对应文件的已知大小，未知时传 -1
     */
    ArchiveResult putStream(String kbId, String docId, String fileName, InputStream input, long knownSize);

    /** 兼容小文件/测试：内部转调 {@link #putStream}。 */
    default StoredObject put(String kbId, String docId, String fileName, byte[] content) {
        return putStream(kbId, docId, fileName, new ByteArrayInputStream(content), content.length).stored();
    }

    /** 按存储键打开原件流（调用方必须 close）。 */
    InputStream openStream(String storageKey);

    /** 读取全部字节（重训等场景；大文件优先 {@link #openStream}）。 */
    byte[] get(String storageKey);

    void delete(String storageKey);

    record StoredObject(String storageKey, String storageUrl, long size) {}

    record ArchiveResult(StoredObject stored, String contentHash) {}
}
