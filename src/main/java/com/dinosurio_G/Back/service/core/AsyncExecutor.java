package com.dinosurio_G.Back.service.core;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class AsyncExecutor {

    @Async("taskExecutor")
    public CompletableFuture<Void> runAsync(Runnable task) {
        task.run();
        return CompletableFuture.completedFuture(null);
    }
}
