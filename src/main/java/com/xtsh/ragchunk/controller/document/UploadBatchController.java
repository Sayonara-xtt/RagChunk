package com.xtsh.ragchunk.controller.document;

import lombok.RequiredArgsConstructor;
import com.xtsh.ragchunk.vo.document.UploadBatchResponse;
import com.xtsh.ragchunk.vo.document.UploadDashboardResponse;
import com.xtsh.ragchunk.service.document.UploadBatchService;
import com.xtsh.ragchunk.service.document.UploadDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "上传批次", description = "批量上传任务查询与上传数据看板")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/knowledge-bases/{kbId}")
public class UploadBatchController {

    private final UploadBatchService uploadBatchService;
    private final UploadDashboardService dashboardService;

    @Operation(summary = "查询批量上传任务", description = "含排队/处理中/成功/失败计数与文档 ID 列表")
    @GetMapping("/upload-batches/{batchId}")
    public UploadBatchResponse getBatch(@PathVariable String kbId, @PathVariable String batchId) {
        return uploadBatchService.getBatch(kbId, batchId);
    }

    @Operation(summary = "上传数据看板", description = "按状态/流程阶段汇总，展示最近批次与文档及流程进度")
    @GetMapping("/upload-dashboard")
    public UploadDashboardResponse dashboard(@PathVariable String kbId) {
        return dashboardService.dashboard(kbId);
    }
}