package com.example.concurrency.dbaccess;

import com.example.concurrency.dbaccess.report.SampleEntry;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class CompletableFuture implements DBManager {

    final int THREADS = Runtime.getRuntime().availableProcessors();
    final ExecutorService executorPool = Executors.newFixedThreadPool(THREADS);

    @Override
    public ExecutorService getExecutorService() {
        return executorPool;
    }

    @Override
    public void insertAction(Long index, HikariDataSource dataSource, Supplier<Void> counterSuccess, Supplier<Void> counterFail, Consumer<SampleEntry> sampleEntryConsumer) {
        LatencyCompletableFuture.track(
            () -> DBQueries.insertUser(index, "User_" + index, dataSource),
            executorPool,
            counterSuccess,
            counterFail,
            sampleEntryConsumer,
            (result) -> log.info("Insert completed: {} -> {}", result.id, result.name)
        );
    }

    @Override
    public void updateAction(
        HikariDataSource dataSource,
        Supplier<Void> counterSuccess,
        Supplier<Void> counterFail,
        Consumer<SampleEntry> sampleEntryConsumer) {

        LatencyCompletableFuture.track(
            () -> {
                long index = Utils.getRandomUserId(dataSource);
                String name = "User_updated_" + index;
                return DBQueries.updateUser(index, name, dataSource);
            },
            executorPool,
            counterSuccess,
            counterFail,
            sampleEntryConsumer,
            user -> log.info("Update completed: {} -> {}", user.id, user.name)
        );
    }

    @Override
    public void selectAction(
        HikariDataSource dataSource,
        Supplier<Void> counterSuccess,
        Supplier<Void> counterFail,
        Consumer<SampleEntry> sampleEntryConsumer) {

        LatencyCompletableFuture.track(
            () -> {
                long index = Utils.getRandomUserId(dataSource);
                return DBQueries.selectUser(index, dataSource);
            },
            executorPool,
            counterSuccess,
            counterFail,
            sampleEntryConsumer,
            user -> log.info("Select completed: {} -> {}", user.id, user.name)
        );
    }
}
