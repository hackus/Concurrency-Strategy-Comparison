package com.example.concurrency.dbaccess;

import com.example.concurrency.dbaccess.report.SampleEntry;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
@RequiredArgsConstructor
public class DbManagerStrategy {

    final DBManager manager;

    public void insert(Long index, HikariDataSource dataSource, Supplier<Void> counterSuccess, Supplier<Void> counterFail, Consumer<SampleEntry> sampleEntryConsumer) {
        manager.insertAction(index, dataSource, counterSuccess, counterFail, sampleEntryConsumer);
    }

    public void update(HikariDataSource dataSource, Supplier<Void> counterSuccess, Supplier<Void> counterFail, Consumer<SampleEntry> sampleEntryConsumer) {
        manager.updateAction(dataSource, counterSuccess, counterFail, sampleEntryConsumer);
    }

    public void select(HikariDataSource dataSource, Supplier<Void> counterSuccess, Supplier<Void> counterFail, Consumer<SampleEntry> sampleEntryConsumer) {
        manager.selectAction(dataSource, counterSuccess, counterFail, sampleEntryConsumer);
    }
}
