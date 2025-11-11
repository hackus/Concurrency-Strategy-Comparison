package com.example.concurrency.threadsleep;

import com.example.concurrency.common.StepContext;
import com.example.concurrency.threadsleep.simulator.TaskSimulator;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SleepStrategyComparisonSteps {

    private static final int TASK_COUNT = 1000;

    // useless, but better
    public static final ThreadLocal<StepContext> context = ThreadLocal.withInitial(StepContext::new);

    private static final Map<String, List<RunResult>> outlineResults =
        new ConcurrentHashMap<>();

    @When("I run the {word} sleep {string}")
    public void runSleepStrategy(String mode, String demoName) {
        StepContext ctx = context.get();
        ctx.mode = mode;
        ctx.demoName = demoName;
        ctx.startNs = System.nanoTime();

        try {
            TaskSimulator strategy = switch (mode.toLowerCase()) {
                case "hard" -> TaskSimulator.hardSleep();
                case "soft" -> TaskSimulator.softSleep();
                default -> throw new IllegalArgumentException("Unknown sleep mode: " + mode);
            };

            // Run the selected demo
            switch (demoName) {
                case "FutureCommonPoolDemo" ->
                    FutureCommonPoolDemo.run(TASK_COUNT, strategy);
                case "FutureExecutorServiceDemo" ->
                    FutureExecutorServiceDemo.run(TASK_COUNT, strategy);
                case "FutureExecutorService1000ThreadsDemo" ->
                    FutureExecutorService1000ThreadsDemo.run(TASK_COUNT, strategy);
                case "CompletableFutureDefaultDemo" ->
                    CompletableFutureDefaultDemo.run(TASK_COUNT, strategy);
                case "CompletableFutureExecutorServiceThreadsDemo" ->
                    CompletableFutureExecutorServiceDemo.run(TASK_COUNT, strategy);
                case "CompletableFutureExecutorService1000ThreadsDemo" ->
                    CompletableFutureExecutorService1000ThreadsDemo.run(TASK_COUNT, strategy);
                case "VirtualThreadDemo" ->
                    VirtualThreadDemo.run(TASK_COUNT, strategy);
                case "ReactiveReactorDemo" ->
                    ReactiveReactorDemo.run(TASK_COUNT, strategy);
                case "ReactiveReactorParallelDemo" ->
                    ReactiveReactorParallelDemo.run(TASK_COUNT, strategy);
                case "ReactiveRxJavaDemo" ->
                    ReactiveRxJavaDemo.run(TASK_COUNT, strategy);
                case "ReactiveRxJavaParallelDemo" ->
                    ReactiveRxJavaParallelDemo.run(TASK_COUNT, strategy);
                default ->
                    throw new IllegalArgumentException("Unknown demo: " + demoName);
            }

            ctx.endNs = System.nanoTime();
            ctx.durationMs = TimeUnit.NANOSECONDS.toMillis(ctx.endNs - ctx.startNs);
            ctx.errorMessage = null;

            System.out.printf("✅ [%s sleep] %s completed in %d ms%n",
                mode, demoName, ctx.durationMs);

        } catch (Throwable ex) {
            ctx.endNs = System.nanoTime();
            ctx.durationMs = TimeUnit.NANOSECONDS.toMillis(ctx.endNs - ctx.startNs);

            String message = String.format("❌ [%s sleep] %s failed after %d ms: %s",
                mode, demoName, ctx.durationMs, ex.getMessage());
            ctx.errorMessage = message;

            // Re-throw to make the scenario fail visibly
            throw new RuntimeException(message, ex);
        }
    }


    @After
    public void collectResults(Scenario scenario) {
        StepContext ctx = context.get();
        if (ctx.demoName == null) return;

        String scenarioNameSignificantPart = scenario.getName().split(" and ")[0];

        outlineResults
            .computeIfAbsent(scenarioNameSignificantPart, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(new RunResult(ctx.mode, ctx.demoName, ctx.startNs, ctx.endNs, ctx.durationMs, ctx.errorMessage));
    }

    @AfterAll
    public static void generateReportsPerOutline() throws IOException {
        if (outlineResults.isEmpty()) return;

        Path dir = Path.of("reports");
        Files.createDirectories(dir);

        for (var entry : outlineResults.entrySet()) {
            String outlineName = entry.getKey();
            List<RunResult> results = entry.getValue();
            results.sort(
                Comparator
                    .comparing(RunResult::isFailed)
                    .thenComparingLong(item -> item.durationMs)
            );

            long best = results.getFirst().durationMs;
            String safeName = outlineName.replaceAll("[^a-zA-Z0-9\\-_]+", "_");

            String html = buildHtmlReport(results, best);
            Path out = Path.of("reports/sleep_strategy/" + safeName + ".html");
            Files.createDirectories(out.getParent());
            Files.writeString(out, html);

            System.out.println("📊 Report generated for outline: " + outlineName);
        }
    }

    private static String buildHtmlReport(List<RunResult> runs, long best) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("""
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>Thread Sleep Strategy Performance Report</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; background: #fafafa; }
                table { border-collapse: collapse; width: 100%; max-width: 900px; margin-top: 20px; }
                th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
                th { background: #0078D7; color: white; }
                tr:nth-child(even) { background: #f9f9f9; }
                h1 { color: #0078D7; }
                footer { margin-top: 20px; color: #555; font-size: 0.9em; }
                .slow { color: #b30000; font-weight: bold; }
                .fast { color: #007800; font-weight: bold; }
                .pass { background: #eaffea; }      /* light green */
                .fail { background: #ffeaea; }      /* light red */
                .error-msg { color: #b30000; font-size: 0.9em; }
            </style>
        </head>
        <body>
            <h1>🧭 Thread Sleep Strategy Performance Report</h1>
            <table>
                <thead>
                    <tr>
                        <th>Strategy</th>
                        <th>Duration (ms)</th>
                        <th>Relative Speed</th>
                        <th>Status</th>
                        <th>Error Message</th>
                    </tr>
                </thead>
                <tbody>
        """);

        for (RunResult r : runs) {
            double ratio = (double) r.durationMs / best;
            String cls = (ratio <= 1.05) ? "fast" : "slow";
            boolean failed = r.errorMessage != null && !r.errorMessage.isBlank();
            String rowClass = failed ? "fail" : "pass";
            String status = failed ? "❌ Fail" : "✅ Pass";
            String errorText = failed ? r.errorMessage.replace("<", "&lt;").replace(">", "&gt;") : "";

            sb.append(String.format(
                "<tr class='%s'><td>%s</td><td>%d</td><td class='%s'>%.2fx</td><td>%s</td><td class='error-msg'>%s</td></tr>%n",
                rowClass, r.demoName, r.durationMs, cls, ratio, status, errorText
            ));
        }

        sb.append(String.format("""
                </tbody>
            </table>
            <p><strong>Best:</strong> %d ms</p>
            <footer>Generated on %s</footer>
        </body>
        </html>
        """, best, timestamp));

        return sb.toString();
    }

    public class RunResult {
        public final String mode;
        public final String demoName;
        public final long startMs;
        public final long endMs;
        public final long durationMs;
        public final String errorMessage;

        public RunResult(String mode, String demoName, long startMs, long endMs, long durationMs, String errorMessage) {
            this.mode = mode;
            this.demoName = demoName;
            this.startMs = startMs;
            this.endMs = endMs;
            this.durationMs = durationMs;
            this.errorMessage = errorMessage;
        }

        public boolean isFailed() {
            return errorMessage != null && !errorMessage.isBlank();
        }
    }
}
