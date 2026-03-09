package com.rreganjr.requel.service.stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Dedicated TaskScheduler for SSE keep-alive and session expiry.
 * Separate from the application's default scheduler to avoid contention.
 */
@Configuration
public class StreamConfig {

    @Bean("streamTaskScheduler")
    public TaskScheduler streamTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("stream-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return scheduler;
    }
}
