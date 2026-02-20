package com.example.tomcat.bench.service;

import com.example.tomcat.bench.model.TxRequest;
import com.example.tomcat.bench.model.TxResponse;
import com.example.tomcat.bench.repository.BenchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TxService {

    private final BenchRepository benchRepository;

    public TxService(BenchRepository benchRepository) {
        this.benchRepository = benchRepository;
    }

    @Transactional
    public TxResponse incrementInTransaction(TxRequest request, int sleepMs) {
        if (sleepMs > 0) {
            benchRepository.sleep(sleepMs);
        }

        int updatedRows = benchRepository.incrementCount(request.id(), request.delta());
        if (updatedRows == 0) {
            throw new IllegalStateException("bench item not found. id=" + request.id());
        }

        long cnt = benchRepository.findCountById(request.id())
                .orElseThrow(() -> new IllegalStateException("bench item not found after update. id=" + request.id()));

        return new TxResponse(request.id(), cnt, request.delta(), sleepMs);
    }
}
