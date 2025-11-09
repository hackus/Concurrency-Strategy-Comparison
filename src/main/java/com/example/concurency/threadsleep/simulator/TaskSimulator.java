package com.example.concurency.threadsleep.simulator;

public class TaskSimulator {
    private final WorkStrategy strategy;

    // Context is configured with a strategy
    public TaskSimulator(WorkStrategy strategy) {
        this.strategy = strategy;
    }

    public String simulateWork(int id, int millis) {
        return strategy.doWork(id, millis);
    }

    // Optional: factory methods for convenience
    public static TaskSimulator hardSleep() {
        return new TaskSimulator(new HardSleepStrategy());
    }

    public static TaskSimulator softSleep() {
        return new TaskSimulator(new SoftSleepStrategy());
    }
}
