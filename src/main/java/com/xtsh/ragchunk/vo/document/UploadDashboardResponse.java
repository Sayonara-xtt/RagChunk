package com.xtsh.ragchunk.vo.document;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "知识库上传数据看板")
@Data

public class UploadDashboardResponse {

    private String kbId;
    private Summary summary;
    private Map<String, Long> byStatus;
    private Map<String, Long> byProcessStage;
    private List<UploadBatchResponse> recentBatches;
    private List<DocumentResponse> recentDocuments;

    @Data

    public static class Summary {
        private long totalDocuments;
        private long queued;
        private long processing;
        private long success;
        private long failed;
        private long totalBatches;

    }

}
