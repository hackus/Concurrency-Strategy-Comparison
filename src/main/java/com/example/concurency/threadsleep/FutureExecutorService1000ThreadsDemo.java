package com.example.concurency.threadsleep;

import com.example.concurency.threadsleep.simulator.TaskSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FutureExecutorService1000ThreadsDemo {

    public static void run(int taskCount, TaskSimulator simulator) throws Exception {
        ExecutorService executor = Executors.newScheduledThreadPool(1000);
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
