package com.example.concurrency.threadsleep;

import com.example.concurrency.threadsleep.simulator.TaskSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompletableFutureExecutorServiceDemo {
    public static void run(int taskCount, TaskSimulator simulator) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < taskCount; i++) {
            int id = i;
            futures.add(
                CompletableFuture.supplyAsync(() -> simulator.simulateWork(id, 500), executor)
                    .thenAccept(System.out::println)
            );
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
