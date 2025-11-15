package com.example.concurrency.dbaccess;

import com.example.concurrency.dbaccess.report.CombinedLatencySeries;
import com.example.concurrency.dbaccess.report.Reporting;
import com.example.concurrency.dbaccess.report.SampleEntry;
import com.example.concurrency.dbaccess.report.Sampler;
import com.sun.management.OperatingSystemMXBean;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.example.concurrency.dbaccess.ZioDBManager;
import com.example.concurrency.dbaccess.CatsDBManager;

@Slf4j
public class DbPerformanceSteps {

    private final Reporting report = new Reporting();
    private HikariDataSource ds;
    OperatingSystemMXBean osBean;

    private CombinedLatencySeries combinedLatencySeries;
    Sampler sampler;
    String normalizedTestName;

    @ParameterType("hikari|CompletableFuture|VirtualThreads|ReactiveRxJavaDBManager|ZioDBManager|CatsDBManager|custom")
    public Class<? extends DBManager> dbManager(String name) {
        return switch (name) {
            case "CompletableFuture" -> CompletableFuture.class;
            case "VirtualThreads" -> VirtualThreads.class;
            case "ReactiveRxJavaDBManager" -> ReactiveRxJavaDBManager.class;
            case "ZioDBManager" -> ZioDBManager.class;
            case "CatsDBManager" -> CatsDBManager.class;
            default -> throw new IllegalArgumentException("Unknown DBManager strategy: " + name);
        };
    }

    public void cleanupH2File() {
        String baseName = "./test";
        String[] files = {baseName + ".mv.db", baseName + ".trace.db"};
        for (String f : files) {
            File file = new File(f);
            if (file.exists()) {
                boolean deleted = file.delete();
                log.info("Deleted " + f + ": " + deleted);
            }
        }
    }

    private void prerequisites() {
        osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        combinedLatencySeries = null;

        sampler = new Sampler();
    }

    @Given("the H2 database {} is initialized and {string} as report path")
    public void setupDb(String databaseName, String reportPath) {
        report.startReport(reportPath);

        cleanupH2File();
        prerequisites();

        HikariConfig config = new HikariConfig();
//        config.setJdbcUrl("jdbc:h2:file:./test;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;LOCK_MODE=0;");
//        config.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;LOCK_MODE=0");
        config.setJdbcUrl("jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1;LOCK_MODE=0");
        config.setUsername("sa");
        config.setPassword("");
        ds = new HikariDataSource(config);

        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("Create TABLE IF NOT EXISTS users (id IDENTITY, name VARCHAR(100), age INT)");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize DB", e);
        }
        log.info("✅ H2 database initialized with 'users' table");
    }

    @When("I run performance tests with {int} concurrent users and {dbManager} strategy and {string} as report path")
    public void runPerformance(int totalUsers, Class<? extends DBManager> dbManagerClass, String reportPath)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, InterruptedException {

        normalizedTestName = normalizeFilename("run performance with " + dbManagerClass.getSimpleName());
        report.startTest(normalizedTestName, "Run performance using: " + dbManagerClass.getSimpleName());

        DBManager dbManager = dbManagerClass.getDeclaredConstructor().newInstance();
        DbManagerStrategy strategy = new DbManagerStrategy(dbManager);

        log.info("Running with " + totalUsers + " users using " + dbManager.getClass().getSimpleName());

        // Split your "users" across the three ops like before

        // --- Profile knobs (tweak as you like) ---
        int rampUpSec = 10;   // seconds to ramp from 0 -> peak
        int steadySec = 20;   // seconds to hold peak
        int rampDownSec = 10;   // seconds to ramp from peak -> 0

        // Peak "requests per second" per operation
        // (Keep reasonable vs. DB/CPU; you can derive from totalUsers if you want)

        // --- Executors ---
        // Worker: runs the actual DB work (CompletableFutures)
        int workerThreads = Math.max(8, Runtime.getRuntime().availableProcessors());

        // Controller: drives the per-second submission for each op
        ExecutorService controller = Executors.newFixedThreadPool(3);

        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();

        // --- Counters wiring remains the same ---
        Supplier<Void> insertSuccessCounter = () -> {
            sampler.insertSuccess.incrementAndGet();
            return null;
        };
        Supplier<Void> insertFailCounter = () -> {
            sampler.insertFail.incrementAndGet();
            return null;
        };
        Consumer<SampleEntry> sampleInsertEntryConsumer = sampler.insertLatencySampleSeries::addSample;

        Consumer<Long> insertStrategy = (Long id) -> {
            strategy.insert(id, ds, insertSuccessCounter, insertFailCounter, sampleInsertEntryConsumer);
        };

        Supplier<Void> updateSuccessCounter = () -> {
            sampler.updateSuccess.incrementAndGet();
            return null;
        };
        Supplier<Void> updateFailCounter = () -> {
            sampler.updateFail.incrementAndGet();
            return null;
        };
        Consumer<SampleEntry> sampleUpdateEntryConsumer = sampler.updateLatencySampleSeries::addSample;
        Consumer<Long> updateStrategy = (Long id) -> {
            strategy.update(ds, updateSuccessCounter, updateFailCounter, sampleUpdateEntryConsumer);
        };

        Supplier<Void> selectSuccessCounter = () -> {
            sampler.selectSuccess.incrementAndGet();
            return null;
        };
        Supplier<Void> selectFailCounter = () -> {
            sampler.selectFail.incrementAndGet();
            return null;
        };
        Consumer<SampleEntry> sampleSelectEntryConsumer = sampler.selectLatencySampleSeries::addSample;
        Consumer<Long> selectStrategy = (Long id) -> {
            strategy.select(ds, selectSuccessCounter, selectFailCounter, sampleSelectEntryConsumer);
        };

        // Per-op id sequences so each op gets unique ids if needed
        AtomicLong insertIdSeq = new AtomicLong(1);
        AtomicLong updateIdSeq = new AtomicLong(1);
        AtomicLong selectIdSeq = new AtomicLong(1);

        // Launch 3 controllers in parallel, each follows the same ramp profile
        List<Callable<Void>> controllers = List.of(
            () -> {
                sampler.driveProfile("INSERT", insertStrategy, totalUsers, rampUpSec, steadySec, rampDownSec, futures, insertIdSeq);
                return null;
            }
            , () -> {
                sampler.driveProfile("UPDATE", updateStrategy, totalUsers, rampUpSec, steadySec, rampDownSec, futures, updateIdSeq);
                return null;
            }
            , () -> {
                sampler.driveProfile("SELECT", selectStrategy, totalUsers, rampUpSec, steadySec, rampDownSec, futures, selectIdSeq);
                return null;
            }
        );

        Supplier<Long> insertSuccessSampler = sampler.insertSuccess::get;
        Supplier<Long> insertFailSampler = sampler.insertFail::get;

        Supplier<Long> updateSuccessSampler = sampler.updateSuccess::get;
        Supplier<Long> updateFailsSampler = sampler.updateFail::get;

        Supplier<Long> selectSuccessSampler = sampler.selectSuccess::get;
        Supplier<Long> selectFailSampler = sampler.selectFail::get;

        Supplier<Double> systemCpuUsageSampler = () -> osBean.getCpuLoad() * 100;
        Supplier<Double> jvmCpuUsageSampler = () -> osBean.getProcessCpuLoad() * 100;

        Supplier<Double> memoryLoad = () -> {
            Runtime rt = Runtime.getRuntime();
            long total = rt.totalMemory();
            long free = rt.freeMemory();
            long used = total - free;
            long max = rt.maxMemory();
            return ((double) used / max) * 100.0;
        };

        sampler.startSamplingPerSecond(
            insertSuccessSampler,
            insertFailSampler,
            updateSuccessSampler,
            updateFailsSampler,
            selectSuccessSampler,
            selectFailSampler,
            systemCpuUsageSampler,
            jvmCpuUsageSampler,
            memoryLoad
        );

        controller.invokeAll(controllers);
        controller.shutdown();

        // Wait for all async DB tasks to finish
        java.util.concurrent.CompletableFuture<Void> all = java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]));
        all.join();

        sampler.stopSamplingPerSecond();

        strategy.manager.getExecutorService().shutdown();
        strategy.manager.getExecutorService().awaitTermination(5, TimeUnit.MINUTES);

        // --- Reporting ---
        Map<String, Tuple2<Long, Long>> reportData = Map.of(
            "insert", Tuples.of(sampler.insertSuccess.get(), sampler.insertFail.get()),
            "update", Tuples.of(sampler.updateSuccess.get(), sampler.updateFail.get()),
            "select", Tuples.of(sampler.selectSuccess.get(), sampler.selectFail.get())
        );

        // Build a time-series bundle (lists are cumulative counts per second)
        Map<String, List<Long>> timeSeries = Map.of(
            "insert_succ", sampler.insertSuccSeries,
            "insert_fail", sampler.insertFailSeries,
            "update_succ", sampler.updateSuccSeries,
            "update_fail", sampler.updateFailSeries,
            "select_succ", sampler.selectSuccSeries,
            "select_fail", sampler.selectFailSeries
        );

        Map<String, List<Double>> timeSeriesSystemLoad = Map.of(
            "systemCpuUsage", sampler.systemCpuUsageSeries,
            "jvmCpuUsage", sampler.jvmCpuUsageSeries,
            "memoryLoad", sampler.memoryLoadSeries
        );

        combinedLatencySeries = new CombinedLatencySeries(sampler.insertLatencySampleSeries, sampler.updateLatencySampleSeries, sampler.selectLatencySampleSeries);

        report.writeReport(reportPath, normalizedTestName, reportData, timeSeries, timeSeriesSystemLoad, combinedLatencySeries);
    }

    @Then("the average {} response time should be under {int} ms and percentage of failed less than {double} percent")
    public void validatePerformance(String check, int averageLatencyThreshold, double failedPercentageThreshold) {
        CombinedLatencySeries.CombinedAvgLatencies combinedAvgLatencies =  combinedLatencySeries.getTotalAverageLatency();
        double averageLatency = 0;
        double failedPercentage = 0;
        if (check.equals("insert")) {
            averageLatency = combinedAvgLatencies.insertAvg();
            failedPercentage = failurePercentage(sampler.insertSuccess, sampler.insertFail);
        }
        if (check.equals("update")) {
            averageLatency = combinedAvgLatencies.updateAvg();
            failedPercentage = failurePercentage(sampler.updateSuccess, sampler.updateFail);
        }
        if (check.equals("select")) {
            averageLatency = combinedAvgLatencies.selectAvg();
            failedPercentage = failurePercentage(sampler.selectSuccess, sampler.selectFail);
        }
        if (averageLatency > averageLatencyThreshold) {
            report.fail(normalizedTestName, "Average latency for " + check + " " + averageLatency + " bigger than threshold: " + averageLatencyThreshold);
        } else {
            report.pass(normalizedTestName, "Average latency for " + check + " " + averageLatency + " corresponds to the threshold: " + averageLatencyThreshold);
        }
        if (failedPercentage > failedPercentageThreshold) {
            report.fail(normalizedTestName, "Percentage for failed " + check + " " + failedPercentage + " bigger than threshold: " + failedPercentageThreshold);
        } else {
            report.pass(normalizedTestName, "Percentage for failed " + check + " " + failedPercentage + " corresponds to the threshold: " + failedPercentageThreshold);
        }

        report.endReport();
    }

    public static String normalizeFilename(String input) {
        if (input == null || input.isBlank()) {
            return "untitled";
        }

        // Replace forbidden characters on Windows and most systems: \ / : * ? " < > |
        String safe = input.replaceAll("[\\\\/:*?\"<>|]", "_");

        // Replace spaces with underscores
        safe = safe.replaceAll("\\s+", "_");

        // Remove control characters and trim spaces
        safe = safe.replaceAll("\\p{Cntrl}", "").trim();

        // Collapse multiple underscores
        safe = safe.replaceAll("_+", "_");

        // Limit to 255 characters (common filesystem limit)
        if (safe.length() > 255) {
            safe = safe.substring(0, 255);
        }

        // Avoid empty result after sanitization
        return safe.isEmpty() ? "untitled" : safe;
    }

    private static double failurePercentage(AtomicLong success, AtomicLong fail) {
        long s = success.get();
        long f = fail.get();
        long total = s + f;
        return total > 0 ? (f * 100.0) / total : 0.0;
    }
}
