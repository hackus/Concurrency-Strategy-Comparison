package com.example.concurency.threadsleep.simulator;

// 1️⃣ Strategy interface
public interface WorkStrategy {
    String doWork(int id, int millis);
}