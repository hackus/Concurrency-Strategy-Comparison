package com.example.concurrency.dbaccess;

import com.example.concurrency.dbaccess.report.SampleEntry;
import com.zaxxer.hikari.HikariDataSource;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface DBManager {

    ExecutorService getExecutorService();

    void insertAction(Long index, HikariDataSource dataSource, Supplier<Void> counterSuccess, Supplier<Void> counterFail, Consumer<SampleEntry> sampleEntryConsumer);

    void updateAction(HikariDataSource dataSource, Supplier<Void> counterSuccess, Supplier<Void> counterFail, Consumer<SampleEntry> sampleEntryConsumer);

    void selectAction(HikariDataSource dataSource, Supplier<Void> counterSuccess, Supplier<Void> counterFail, Consumer<SampleEntry> sampleEntryConsumer);


}
