package com.xtsh.ragchunk.dto.document;

/**
 * 文档离线建库流程阶段（异步上传可查询当前处于哪一步）。
 */
public enum DocumentProcessStage {

    /** 已入队，等待线程池 */
    QUEUED(0, "排队中"),
    /** 原件写入 OSS/本地归档 */
    OSS_ARCHIVING(10, "原件归档"),
    /** 解析与文本规范化 */
    PARSING(30, "解析正文"),
    /** 规则/混合切片 */
    CHUNKING(55, "混合切片"),
    /** 向量化写入 */
    EMBEDDING(80, "向量化入库"),
    /** 完成 */
    SUCCESS(100, "完成"),
    /** 失败 */
    FAILED(0, "失败");

    private final int defaultProgress;
    private final String label;

    DocumentProcessStage(int defaultProgress, String label) {
        this.defaultProgress = defaultProgress;
        this.label = label;
    }

    public int defaultProgress() {
        return defaultProgress;
    }

    public String label() {
        return label;
    }

    public static DocumentProcessStage fromCode(String code) {
        if (code == null || code.isBlank()) {
            return QUEUED;
        }
        return DocumentProcessStage.valueOf(code);
    }
}
