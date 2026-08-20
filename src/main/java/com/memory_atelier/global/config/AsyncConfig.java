package com.memory_atelier.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// 에디션 생성 파이프라인처럼 수십 초 이상 걸리는 작업을 HTTP 요청 스레드 밖에서 돌리기 위한 설정
// 풀 크기를 제한해 두는 이유는, AI 서버 호출이 몰릴 때 스레드가 무한정 늘어나 커넥션 풀·메모리를 고갈시키는 걸 막기 위함이다
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "editionPipelineExecutor")
    public Executor editionPipelineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("edition-pipeline-");
        executor.initialize();
        return executor;
    }
}
