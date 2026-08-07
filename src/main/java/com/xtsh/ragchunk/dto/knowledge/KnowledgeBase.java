package com.xtsh.ragchunk.dto.knowledge;

import lombok.Data;
import java.time.Instant;

@Data

public class KnowledgeBase {
    private String id;
    private String name;
    private String description;
    private String status;
    private KnowledgeBaseConfig config;
    private Instant createdAt;

}
