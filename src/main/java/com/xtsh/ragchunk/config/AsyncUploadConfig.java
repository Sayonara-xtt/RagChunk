package com.xtsh.ragchunk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 文档异步入库线程池（批量上传、可重复训练）。
 */
@Configuration
@EnableAsync
public class AsyncUploadConfig {

    @Bean(name = "documentIngestTaskExecutor")
    public Executor documentIngestTaskExecutor(RagChunkProperties properties) {
        var upload = properties.getUpload();
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(upload.getCorePoolSize());
        ex.setMaxPoolSize(upload.getMaxPoolSize());
        ex.setQueueCapacity(upload.getQueueCapacity());
        ex.setThreadNamePrefix("doc-ingest-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.initialize();
        return ex;
    }
}
