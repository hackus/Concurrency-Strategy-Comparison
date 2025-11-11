package com.example.concurrency.threadsleep;

import com.example.concurrency.threadsleep.simulator.TaskSimulator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ReactiveReactorParallelDemo {

    public static void run(int taskCount, TaskSimulator simulator) {
        Flux.range(1, taskCount)
            .flatMap(
                id -> Mono.fromCallable(() -> simulator.simulateWork(id, 500))
                    .subscribeOn(Schedulers.parallel()),
                taskCount
            )
            .doOnNext(System.out::println)
            .blockLast();
    }
}
