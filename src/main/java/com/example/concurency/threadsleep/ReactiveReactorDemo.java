package com.example.concurency.threadsleep;

import com.example.concurency.threadsleep.simulator.TaskSimulator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ReactiveReactorDemo {

    public static void run(int taskCount, TaskSimulator simulator) {
        long start = System.currentTimeMillis();

        Flux.range(1, taskCount)
            .flatMap(id ->
                Mono.fromCallable(() -> simulator.simulateWork(id, 500))
                    .subscribeOn(Schedulers.boundedElastic()) // offload blocking work
            )
            .doOnNext(System.out::println)
            .blockLast(); // Wait for all tasks to complete

        System.out.println("ReactiveDemo took: " + (System.currentTimeMillis() - start) + " ms");
    }
}
