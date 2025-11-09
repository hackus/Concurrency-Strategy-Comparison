package com.example.concurency.pdfreader;

import com.example.concurency.common.StepContext;
import com.example.concurency.pdfreader.*;
import io.cucumber.java.*;
import io.cucumber.java.en.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

public class PdfReaderSteps {

    private Path pdfPath;
    private Path expectedPath;
    private final Path outputPath = Path.of("extracted_text.txt");
    private static final List<RunResult> allRuns = new ArrayList<>();
    // useless, but better
    public static final ThreadLocal<StepContext> context = ThreadLocal.withInitial(StepContext::new);

    record RunResult(String name, long startMs, long endMs, long durationMs) {}

    @Before
    public void setup() throws Exception {
        PdfLogSilencer.silencePdfBoxWarnings();
    }

    @Given("a PDF file {string} and expected output {string}")
    public void givenPdfFiles(String pdfName, String expectedName) throws URISyntaxException {
        pdfPath = Paths.get(Objects.requireNonNull(
            getClass().getClassLoader().getResource(pdfName)).toURI());
        expectedPath = Paths.get(
            Objects.requireNonNull(
                getClass().getClassLoader().getResource(expectedName)
            ).toURI()
        );
    }

    @Given("any existing {string} file is deleted")
    public void deleteExistingFile(String fileName) throws IOException {
        Path path = Path.of(fileName);
        if (Files.exists(path)) {
            Files.delete(path);
            System.out.println("✅ Old " + fileName + " deleted.");
        }
    }

    @Given("PDFBox warnings are silenced")
    public void silencePdfBox() {
        PdfLogSilencer.silencePdfBoxWarnings();
    }

    @When("I run the {string}")
    public void runPdfReader(String demoName) throws Exception {
        StepContext ctx = context.get();
        ctx.demoName = demoName;
        ctx.startNs = System.nanoTime();

        switch (demoName) {
            case "FuturePdfReaderDemo" ->
                FuturePdfReaderDemo.run(pdfPath.toString());
            case "CompletableFuturePdfReaderBlockingDemo" ->
                CompletableFuturePdfReaderBlockingDemo.run(pdfPath.toString());
            case "VirtualThreadPdfReaderDemo" ->
                VirtualThreadPdfReaderDemo.run(pdfPath.toString());
            case "CompletableFuturePdfReaderNonBlockingDemo" -> {
                CompletableFuture<?> cf = CompletableFuturePdfReaderNonBlockingDemo.run(pdfPath.toString());
                cf.join();
            }
            case "RxJavaPdfReaderDemo" ->
                RxJavaPdfReaderDemo.run(pdfPath.toString()).blockingAwait();
            default -> throw new IllegalArgumentException("Unknown demo: " + demoName);
        }

        ctx.endNs = System.nanoTime();
        ctx.durationMs = TimeUnit.NANOSECONDS.toMillis(ctx.endNs - ctx.startNs);

        allRuns.add(new RunResult(
            ctx.demoName,
            TimeUnit.NANOSECONDS.toMillis(ctx.startNs),
            TimeUnit.NANOSECONDS.toMillis(ctx.endNs),
            ctx.durationMs
        ));

        System.out.printf("🕒 %s completed in %d ms%n", ctx.demoName, ctx.durationMs);
    }

    @Then("the output text should match the expected text")
    public void verifyOutputMatches() throws IOException {
        byte[] actual = Files.readAllBytes(outputPath);
        byte[] expected = Files.readAllBytes(expectedPath);
        assertArrayEquals(expected, actual, "Output text differs from expected text");
    }

    @AfterAll
    public static void generateHtmlReport() throws IOException {
        if (allRuns.isEmpty()) return;

        allRuns.sort(Comparator.comparingLong(RunResult::durationMs));
        long best = allRuns.get(0).durationMs();

        Path reportPath = Path.of("reports/pdf_reader/pdf_reader_report.html");
        try (BufferedWriter writer = Files.newBufferedWriter(reportPath)) {
            writer.write("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>PDF Reader Performance Report</title>
                  <style>
                    body {
                      font-family: Arial, sans-serif;
                      margin: 40px;
                      background: #f6f8fa;
                      color: #333;
                    }
                    h1 {
                      text-align: center;
                      color: #1a73e8;
                    }
                    table {
                      width: 100%;
                      border-collapse: collapse;
                      margin-top: 20px;
                      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                      background: #fff;
                    }
                    th, td {
                      padding: 12px 16px;
                      border-bottom: 1px solid #ddd;
                      text-align: left;
                    }
                    th {
                      background: #eaf1fb;
                    }
                    tr:hover {
                      background: #f1f7ff;
                    }
                    .fastest {
                      background: #c8e6c9;
                    }
                    .slower {
                      color: #b71c1c;
                    }
                    footer {
                      text-align: center;
                      margin-top: 30px;
                      font-size: 0.9em;
                      color: #666;
                    }
                  </style>
                </head>
                <body>
                  <h1>📊 PDF Reader Performance Report</h1>
                  <table>
                    <thead>
                      <tr>
                        <th>Test Name</th>
                        <th>Start (ms)</th>
                        <th>End (ms)</th>
                        <th>Duration (ms)</th>
                        <th>Relative Speed</th>
                      </tr>
                    </thead>
                    <tbody>
                """);

            for (RunResult r : allRuns) {
                double ratio = (double) r.durationMs() / best;
                String speedLabel = ratio == 1.0 ? "🏆 Fastest" :
                    String.format("%.2fx slower", ratio);
                boolean isFastest = ratio == 1.0;
                writer.write(String.format("""
                    <tr class="%s">
                      <td>%s</td>
                      <td>%d</td>
                      <td>%d</td>
                      <td>%d</td>
                      <td class="%s">%s</td>
                    </tr>
                    """,
                    isFastest ? "fastest" : "",
                    r.name(), r.startMs(), r.endMs(), r.durationMs(),
                    isFastest ? "" : "slower",
                    speedLabel
                ));
            }

            writer.write("""
                    </tbody>
                  </table>
                  <footer>Generated by PdfReaderSteps at %s</footer>
                </body>
                </html>
                """.formatted(new java.util.Date()));
        }

        System.out.println("📁 HTML report saved to: " + reportPath.toAbsolutePath());
    }
}
