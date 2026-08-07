package com.xtsh.ragchunk.dto.chat;

import lombok.Data;
import com.xtsh.ragchunk.service.chat.QaScheme;

/**
 * 单次问答运行统计（写入响应 meta，便于联调与审计）。
 */
@Data

public class ChatRunStats {

    private QaScheme scheme;
    private int llmCalls;
    private int searchRounds;
    private boolean rewriteTriggered;
    private int hitCount;
    private double maxScore;

    public void incrementLlmCalls() { this.llmCalls++; }
    public void incrementSearchRounds() { this.searchRounds++; }
}
