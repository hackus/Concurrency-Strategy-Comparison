package com.example.concurency.threadsleep;

import com.example.concurency.threadsleep.simulator.TaskSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompletableFutureExecutorService1000ThreadsDemo {
    public static void run(int taskCount, TaskSimulator simulator) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newScheduledThreadPool(1000);

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
