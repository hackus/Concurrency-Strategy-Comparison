package com.example.concurrency.threadsleep;

import com.example.concurrency.threadsleep.simulator.TaskSimulator;

import java.util.*;
import java.util.concurrent.*;

public class FutureExecutorServiceDemo {

    public static void run(int taskCount, TaskSimulator simulator) throws Exception {
        ExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<String>> futures = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            int id = i;
            futures.add(executor.submit(() -> simulator.simulateWork(id, 500)));
        }

        for (Future<String> f : futures) {
            System.out.println(f.get());
        }

        executor.shutdown();
        System.out.println("FutureDemo took: " + (System.currentTimeMillis() - start) + " ms");
    }
}
