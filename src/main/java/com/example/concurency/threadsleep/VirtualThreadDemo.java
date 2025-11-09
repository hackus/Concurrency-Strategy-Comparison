package com.example.concurency.threadsleep;

import com.example.concurency.threadsleep.simulator.TaskSimulator;

import java.util.*;
import java.util.concurrent.*;


public class VirtualThreadDemo {
    public static void run(int taskCount, TaskSimulator simulator) throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                int id = i;
                executor.execute(() -> {
                    simulator.simulateWork(id, 500);
                    System.out.println("Task " + id + " done by " + Thread.currentThread());
                });
            }
        }
    }
}