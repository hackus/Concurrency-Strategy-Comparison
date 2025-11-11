package com.example.concurrency.threadsleep;

import com.example.concurrency.threadsleep.simulator.TaskSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CompletableFutureDefaultDemo {
    public static void run(int taskCount, TaskSimulator simulator) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            int id = i;
            futures.add(
                CompletableFuture.supplyAsync(() -> simulator.simulateWork(id, 500))
                    .thenAccept(System.out::println)
            );
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
