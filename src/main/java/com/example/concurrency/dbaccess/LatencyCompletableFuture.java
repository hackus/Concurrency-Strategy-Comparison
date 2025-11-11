package com.example.concurrency.dbaccess;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.*;

import com.example.concurrency.dbaccess.report.SampleEntry;
import lombok.extern.slf4j.Slf4j;
import reactor.util.function.Tuples;

@Slf4j
public class LatencyCompletableFuture {

    /**
     * Runs a task asynchronously with timing and unified success/failure handling.
     */
    public static <T> void track(
        Supplier<Optional<T>> task,
        Executor executor,
        Supplier<Void> counterSuccess,
        Supplier<Void> counterFail,
        Consumer<SampleEntry> sampleConsumer,
        Consumer<T> onSuccess) {

        long fallbackStartNs = System.nanoTime();

        java.util.concurrent.CompletableFuture
            .supplyAsync(() -> {
                long startNs = System.nanoTime();
                try {
                    Optional<T> result = task.get();
                    return Tuples.of(result, startNs);
                } catch (Exception ex) {
                    throw new AsyncExecutionException(startNs, ex);
                }
            }, executor)
            .handle((tuple, ex) -> {
                long startNs;
                Optional<T> result = Optional.empty();

                if (ex == null) {
                    startNs = tuple.getT2();
                    result = tuple.getT1();

                    result.ifPresentOrElse(item -> {
                        counterSuccess.get();
                        onSuccess.accept(item);
                    }, counterFail::get);
                } else {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof AsyncExecutionException te) {
                        startNs = te.startNs;
                        log.error("Async task failed (timed): {}", te.getCause().getMessage());
                    } else {
                        startNs = fallbackStartNs;
                        log.error("Async task failed: {}", cause.getMessage());
                    }
                    counterFail.get();
                }

                long endNs = System.nanoTime();
                sampleConsumer.accept(new SampleEntry(startNs, endNs - startNs));
                return null;
            });
    }

    // --- helper class to carry start time across exceptions ---
    public static class AsyncExecutionException extends RuntimeException {
        final long startNs;

        AsyncExecutionException(long startNs, Throwable cause) {
            super(cause);
            this.startNs = startNs;
        }
    }
}
