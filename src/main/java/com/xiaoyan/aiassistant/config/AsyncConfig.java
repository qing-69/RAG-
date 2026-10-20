package com.xiaoyan.aiassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// 异步任务配置，文档入库走独立小线程池，避免占用默认执行器。
@Configuration
@EnableAsync
public class AsyncConfig {

    // 文档解析入库专用线程池，2 个线程足够覆盖培训场景的上传频率。
    @Bean
    public Executor documentIngestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("doc-ingest-");
        executor.initialize();
        return executor;
    }
}
