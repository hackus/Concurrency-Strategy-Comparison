package com.example.concurency.threadsleep.simulator;

// 2️⃣ Concrete strategy: Hard sleep
public class HardSleepStrategy implements WorkStrategy {
    @Override
    public String doWork(int id, int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String str = "Task " + id + " done by " + Thread.currentThread().getName();
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return str;
    }
}
