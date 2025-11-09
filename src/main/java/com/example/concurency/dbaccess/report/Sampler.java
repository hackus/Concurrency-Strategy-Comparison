package com.example.concurency.dbaccess.report;

import com.sun.management.OperatingSystemMXBean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
@RequiredArgsConstructor
public class Sampler {

    OperatingSystemMXBean osBean =
        (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public AtomicLong insertSuccess = new AtomicLong();
    public AtomicLong insertFail = new AtomicLong();
    public AtomicLong updateSuccess = new AtomicLong();
    public AtomicLong updateFail = new AtomicLong();
    public AtomicLong selectSuccess = new AtomicLong();
    public AtomicLong selectFail = new AtomicLong();

    long benchmarkStartNs = System.nanoTime();
    public SampleSeries insertLatencySampleSeries = new SampleSeries(benchmarkStartNs);
    public SampleSeries updateLatencySampleSeries = new SampleSeries(benchmarkStartNs);
    public SampleSeries selectLatencySampleSeries = new SampleSeries(benchmarkStartNs);

    // Per-second snapshots
    public final List<Long> insertSuccSeries = new ArrayList<>();
    public final List<Long> insertFailSeries = new ArrayList<>();
    public final List<Long> updateSuccSeries = new ArrayList<>();
    public final List<Long> updateFailSeries = new ArrayList<>();
    public final List<Long> selectSuccSeries = new ArrayList<>();
    public final List<Long> selectFailSeries = new ArrayList<>();

    public final List<Double> systemCpuUsageSeries = new ArrayList<>();
    public final List<Double> jvmCpuUsageSeries = new ArrayList<>();
    public final List<Double> memoryLoadSeries = new ArrayList<>();

    // Returns the RPS at a given second based on rampUp/steady/rampDown (linear ramp)
    private int rpsForSecond(int second,
                             int peakRps,
                             int rampUpSec,
                             int steadySec,
                             int rampDownSec) {

        if (second < rampUpSec) {
            // 0 -> peak (exclusive of peak)
            return Math.max(0, (int) Math.round( ((double) (second + 1) / rampUpSec) * peakRps ));
        }

        if (second < rampUpSec + steadySec - 1) {
            // steady
            return peakRps;
        }

        int downSecond = second - (rampUpSec + steadySec - 1);
        if (downSecond < rampDownSec) {
            // peak -> 0
            int remaining = rampDownSec - downSecond;
            return Math.max(0, (int) Math.round( ((double) remaining / rampDownSec) * peakRps ));
        }

        return 0;
    }

    /**
     * Drive a time-based load profile for one operation type.
     * - Submits tasks to 'worker' executor at a per-second rate
     * - Stops when either time is over or 'cap' tasks were submitted
     */
    public void driveProfile(String name,
                             Consumer<Long> strategySubmitter,
                             int peakRps,               // peak requests per second
                             int rampUpSec,
                             int steadySec,
                             int rampDownSec,
                             List<CompletableFuture<Void>> futures,
                             AtomicLong idSeq           // unique ids for submitter
    ) {
        final int totalSeconds = rampUpSec + steadySec + rampDownSec;

        final long startNs = System.nanoTime();
        final long totalDurationNs = TimeUnit.SECONDS.toNanos(totalSeconds);
        final long oneSecond = TimeUnit.SECONDS.toNanos(1);
        long elapsedNs = 0;

        while(elapsedNs <= totalDurationNs) {

            if (elapsedNs >= totalDurationNs) break;

            int sec = (int) TimeUnit.NANOSECONDS.toSeconds(elapsedNs);
            int rps = rpsForSecond(sec, peakRps, rampUpSec, steadySec, rampDownSec);
            int toSubmit = rps;

            final long startSec = System.nanoTime();
            long elapsedTime = 0;
            for (int i = 0; i < toSubmit; i++) {
                final long id = idSeq.getAndIncrement();

                strategySubmitter.accept(id);

                final long endSec = System.nanoTime();
                elapsedTime = endSec - startSec;
                if(oneSecond <= elapsedTime) break;
            }

            if(oneSecond > elapsedTime) {
                try {
                    TimeUnit.NANOSECONDS.sleep(oneSecond - elapsedTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            elapsedNs = System.nanoTime() - startNs;
        }
    }

    private ScheduledExecutorService samplerExec;

    public void startSamplingPerSecond(
        Supplier<Long> insertSuccess,
        Supplier<Long> insertFail,
        Supplier<Long> updateSuccess,
        Supplier<Long> updateFail,
        Supplier<Long> selectSuccess,
        Supplier<Long> selectFail,
        Supplier<Double> systemCpuUsageSampler,
        Supplier<Double> jvmCpuUsageSampler,
        Supplier<Double> memoryLoad
    ) {
        samplerExec = Executors.newSingleThreadScheduledExecutor();
        samplerExec.scheduleAtFixedRate(() -> {
            // snapshot cumulative totals each second
            insertSuccSeries.add(insertSuccess.get());
            insertFailSeries.add(insertFail.get());
            updateSuccSeries.add(updateSuccess.get());
            updateFailSeries.add(updateFail.get());
            selectSuccSeries.add(selectSuccess.get());
            selectFailSeries.add(selectFail.get());
            systemCpuUsageSeries.add(systemCpuUsageSampler.get());
            jvmCpuUsageSeries.add(jvmCpuUsageSampler.get());
            memoryLoadSeries.add(memoryLoad.get());
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stopSamplingPerSecond() {
        if (samplerExec != null) {
            samplerExec.shutdown();
            try { samplerExec.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
    }
}
