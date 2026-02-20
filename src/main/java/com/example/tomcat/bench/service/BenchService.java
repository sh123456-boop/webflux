package com.example.tomcat.bench.service;

import com.example.tomcat.bench.model.BenchItem;
import com.example.tomcat.bench.model.DbReadResponse;
import com.example.tomcat.bench.model.TxRequest;
import com.example.tomcat.bench.model.TxResponse;
import com.example.tomcat.bench.repository.BenchRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
public class BenchService {

    private final BenchRepository benchRepository;
    private final TxService txService;
    private final Scheduler jdbcScheduler;

    public BenchService(
            BenchRepository benchRepository,
            TxService txService,
            @Qualifier("jdbcOffloadScheduler") Scheduler jdbcScheduler
    ) {
        this.benchRepository = benchRepository;
        this.txService = txService;
        this.jdbcScheduler = jdbcScheduler;
    }

    public Mono<DbReadResponse> readWithSleep(long id, int sleepMs) {
        return Mono.fromCallable(() -> {
                    validateSleepMs(sleepMs);
                    benchRepository.sleep(sleepMs);

                    BenchItem item = benchRepository.findById(id)
                            .orElseThrow(() -> new IllegalStateException("bench item not found. id=" + id));

                    return new DbReadResponse(item.id(), item.payload(), item.cnt(), sleepMs);
                })
                .subscribeOn(jdbcScheduler);
    }

    public Mono<TxResponse> incrementInTransaction(TxRequest request, int sleepMs) {
        return Mono.fromCallable(() -> {
                    validateSleepMs(sleepMs);
                    return txService.incrementInTransaction(request, sleepMs);
                })
                .subscribeOn(jdbcScheduler);
    }

    private void validateSleepMs(int sleepMs) {
        if (sleepMs < 0) {
            throw new IllegalArgumentException("sleepMs must be >= 0");
        }
    }
}
