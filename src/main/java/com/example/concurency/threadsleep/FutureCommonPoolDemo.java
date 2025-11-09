package com.example.concurency.threadsleep;

import com.example.concurency.threadsleep.simulator.TaskSimulator;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;

public class FutureCommonPoolDemo {
    public static void run(int taskCount, TaskSimulator simulator) throws Exception {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        Semaphore limiter = new Semaphore(pool.getParallelism() * 2);

        for (int i = 0; i < taskCount; i++) {
            int id = i;
            limiter.acquire();
            pool.submit(() -> {
                try {
                    simulator.simulateWork(id, 500);
                } finally {
                    limiter.release();
                }
            });
        }
    }
}