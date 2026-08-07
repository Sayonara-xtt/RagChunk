package com.xtsh.ragchunk.service.document;

/**
 * 异步入库任务：原件已在请求线程流式归档至 OSS，后台仅解析/切片/向量。
 */
public record DocumentIngestJob(
        String docId,
        String kbId,
        String batchId,
        String fileName,
        boolean smartChunk,
        String sourceType,
        boolean retrain
) {
}
