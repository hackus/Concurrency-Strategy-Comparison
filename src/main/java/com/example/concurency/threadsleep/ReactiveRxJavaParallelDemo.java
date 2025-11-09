package com.example.concurency.threadsleep;

import com.example.concurency.threadsleep.simulator.TaskSimulator;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ReactiveRxJavaParallelDemo {

    public static void run(int taskCount, TaskSimulator simulator) {
        Flowable.range(0, taskCount)
            .flatMap(id ->
                    Single.fromCallable(() -> simulator.simulateWork(id, 500))
                        .subscribeOn(Schedulers.io())
                        .toFlowable(),
                Runtime.getRuntime().availableProcessors()
            )
            .doOnNext(System.out::println)
            .blockingSubscribe();
    }
}
