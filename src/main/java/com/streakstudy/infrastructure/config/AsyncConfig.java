package com.streakstudy.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.streakstudy.infrastructure.email.EmailProperties;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(EmailProperties.class)
public class AsyncConfig {

    @Bean(name = "pdfProcessorExecutor")
    public Executor pdfProcessorExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(4);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("pdf-proc-");
        exec.initialize();
        return exec;
    }

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(4);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("email-");
        exec.initialize();
        return exec;
    }
}
