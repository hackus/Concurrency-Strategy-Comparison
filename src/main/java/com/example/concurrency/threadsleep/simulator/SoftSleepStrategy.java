package com.example.concurrency.threadsleep.simulator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SoftSleepStrategy implements WorkStrategy {

    private final static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public String doWork(int id, int millis) {

        CompletableFuture<String> resultFuture = new CompletableFuture<>();

        scheduler.schedule(() -> {
            resultFuture.complete("Task " + id + " done by " + Thread.currentThread().getName());
        }, millis, TimeUnit.MILLISECONDS);

        try {
            return resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
