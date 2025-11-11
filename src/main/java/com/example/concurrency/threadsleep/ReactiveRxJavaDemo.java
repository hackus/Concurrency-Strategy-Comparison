package com.example.concurrency.threadsleep;

import com.example.concurrency.threadsleep.simulator.TaskSimulator;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ReactiveRxJavaDemo {

    public static void run(int taskCount, TaskSimulator simulator) {
        long start = System.currentTimeMillis();

        Flowable.range(1, taskCount)
            .flatMap(id ->
                Single.fromCallable(() -> simulator.simulateWork(id, 500))
                    .subscribeOn(Schedulers.io())
                    .toFlowable()
            )
            .doOnNext(System.out::println)
            .blockingSubscribe(); // Wait until all tasks complete

        System.out.println("ReactiveRxJavaDemo took: " + (System.currentTimeMillis() - start) + " ms");
    }
}
