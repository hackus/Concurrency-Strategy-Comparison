package com.example.concurency.dbaccess;

import com.example.concurency.dbaccess.report.SampleEntry;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.example.concurency.dbaccess.DBQueries.insertUser;
import static com.example.concurency.dbaccess.DBQueries.updateUser;

@Slf4j
public class VirtualThreads implements DBManager {

    ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("thread-", 0).factory());

    @Override
    public ExecutorService getExecutorService() {
        return executor;
    }

    @Override
    public void insertAction(Long index,
                             HikariDataSource dataSource,
                             Supplier<Void> counterSuccess,
                             Supplier<Void> counterFail,
                             Consumer<SampleEntry> sampleEntryConsumer) {

        executor.submit(() -> {
            final long startNs = System.nanoTime();
            boolean actionFailed = false;
            try {
                String name = "User_" + index;
                if(insertUser(index, name, dataSource).isPresent()) {
                    counterSuccess.get();
                } else {
                    actionFailed = true;
                }
                log.info("Insert completed: {} -> {}", index, name);
            } catch (Exception ex) {
                actionFailed = true;
                log.error("Insert failed: {}", ex.getMessage(), ex);
            }
            if(actionFailed)
                counterFail.get();
            final long endNs = System.nanoTime();
            sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs));
        });
    }

    @Override
    public void updateAction(HikariDataSource dataSource,
                             Supplier<Void> counterSuccess,
                             Supplier<Void> counterFail,
                             Consumer<SampleEntry> sampleEntryConsumer) {
        executor.submit(() -> {
            final long startNs = System.nanoTime();
            boolean actionFailed = false;
            try {
                long index = Utils.getRandomUserId(dataSource);
                String name = "User_updated_" + index;
                if(updateUser(index, name, dataSource).isPresent()) {
                    counterSuccess.get();
                    log.info("Update completed: {} -> {}", index, name);
                } else {
                    actionFailed = true;
                }
            } catch (Exception ex) {
                actionFailed = true;
                log.error("Update failed: {}", ex.getMessage(), ex);
            }
            if(actionFailed)
                counterFail.get();
            final long endNs = System.nanoTime();
            sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs));
        });
    }

    @Override
    public void selectAction(HikariDataSource dataSource,
                             Supplier<Void> counterSuccess,
                             Supplier<Void> counterFail,
                             Consumer<SampleEntry> sampleEntryConsumer) {
        executor.submit(() -> {
            final long startNs = System.nanoTime();
            boolean actionFailed = false;
            try {
                long index = Utils.getRandomUserId(dataSource);
                if(DBQueries.selectUser(index, dataSource).isPresent()) {
                    counterSuccess.get();
                } else {
                    actionFailed = true;
                }
                log.info("Select completed: {}", index);
            } catch (Throwable ex) {
                actionFailed = true;
                log.error("Select failed: {}", ex.getMessage(), ex);
            }
            if(actionFailed)
                counterFail.get();
            final long endNs = System.nanoTime();
            sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs));
        });
    }
}
