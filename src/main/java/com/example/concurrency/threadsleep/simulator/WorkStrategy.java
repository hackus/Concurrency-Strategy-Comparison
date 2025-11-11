package com.example.concurrency.threadsleep.simulator;

public interface WorkStrategy {
    String doWork(int id, int millis);
}