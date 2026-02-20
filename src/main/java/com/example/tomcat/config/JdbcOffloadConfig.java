package com.example.tomcat.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class JdbcOffloadConfig {

    @Bean(name = "jdbcOffloadExecutor", destroyMethod = "shutdown")
    public ExecutorService jdbcOffloadExecutor(
            @Value("${app.jdbcOffload.threads:50}") int threads
    ) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger idx = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("jdbc-offload-" + idx.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        };
        return Executors.newFixedThreadPool(threads, threadFactory);
    }

    @Bean(name = "jdbcOffloadScheduler", destroyMethod = "dispose")
    public Scheduler jdbcOffloadScheduler(ExecutorService jdbcOffloadExecutor) {
        return Schedulers.fromExecutorService(jdbcOffloadExecutor);
    }
}
