package com.example.concurency.threadsleep.simulator;

public interface WorkStrategy {
    String doWork(int id, int millis);
}