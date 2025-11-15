package com.example.concurrency.dbaccess;

import com.example.concurrency.dbaccess.report.SampleEntry;
import com.zaxxer.hikari.HikariDataSource;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class ReactiveRxJavaDBManager implements DBManager {

    private final int THREADS = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executorPool = Executors.newFixedThreadPool(THREADS);

    @Override
    public ExecutorService getExecutorService() {
        return executorPool;
    }

    @Override
    public void insertAction(
        Long index,
        HikariDataSource dataSource,
        Supplier<Void> counterSuccess,
        Supplier<Void> counterFail,
        Consumer<SampleEntry> sampleEntryConsumer) {

        long startNs = System.nanoTime();

        Single.fromCallable(() -> DBQueries.insertUser(index, "User_" + index, dataSource))
            .subscribeOn(Schedulers.from(executorPool))
            .doOnSuccess(result -> {
                counterSuccess.get();
                long endNs = System.nanoTime();
                sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs));
                log.info("Insert completed: {}", UserInfo.getAsString(result));
            })
            .doOnError(e -> {
                counterFail.get();
                long endNs = System.nanoTime();
                sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs));
                log.error("Insert failed for {}: {}", index, e.toString());
            })
            .subscribe();
    }

    @Override
    public void updateAction(
        HikariDataSource dataSource,
        Supplier<Void> counterSuccess,
        Supplier<Void> counterFail,
        Consumer<SampleEntry> sampleEntryConsumer) {

        long startNs = System.nanoTime();
        long index = Utils.getRandomUserId(dataSource);

        Single.fromCallable(() -> {
                String name = "User_updated_" + index;
                return DBQueries.updateUser(index, name, dataSource);
            })
            .subscribeOn(Schedulers.from(executorPool))
            .doOnSuccess(result -> {
                counterSuccess.get();
                long endNs = System.nanoTime();
                sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs));
                log.info("Update completed: {}", UserInfo.getAsString(result));
            })
            .doOnError(e -> {
                counterFail.get();
                long endNs = System.nanoTime();
                sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs));
                log.error("Update failed for {}: {}", index, e.toString());
            })
            .subscribe();
    }

    @Override
    public void selectAction(
        HikariDataSource dataSource,
        Supplier<Void> counterSuccess,
        Supplier<Void> counterFail,
        Consumer<SampleEntry> sampleEntryConsumer) {

        long startNs = System.nanoTime();
        long index = Utils.getRandomUserId(dataSource);

        Single.fromCallable(() -> DBQueries.selectUser(index, dataSource))
            .subscribeOn(Schedulers.from(executorPool))
            .doOnSuccess(result -> {
                counterSuccess.get();
                long endNs = System.nanoTime();
                sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs));
                log.info("Select completed: {}", UserInfo.getAsString(result));
            })
            .doOnError(e -> {
                counterFail.get();
                long endNs = System.nanoTime();
                sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs));
                log.error("Update failed for {}: {}", index, e.toString());
            })
            .subscribe();
    }
}
