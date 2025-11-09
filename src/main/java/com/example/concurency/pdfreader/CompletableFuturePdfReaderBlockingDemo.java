package com.example.concurency.pdfreader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CompletableFuturePdfReaderBlockingDemo {

    public static void run(String localPdfPath) throws Exception {

        final int THREADS = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        Path outputPath = Path.of("extracted_text.txt");
        Path expectedPath = Path.of("expected_extracted_text.txt");

        // Delete old file if exists
        if (Files.exists(outputPath)) {
            Files.delete(outputPath);
            System.out.println("Old extracted_text.txt deleted.");
        }

        // 1️⃣ Load PDF
        PDDocument document = PDDocument.load(new File(localPdfPath));
        int totalPages = document.getNumberOfPages();
        int chunkSize = Math.max(10, totalPages / THREADS);

        // 2️⃣ Create a CompletableFuture for each page chunk
        List<CompletableFuture<String>> futures = new ArrayList<>();

        List<int[]> chunks = IntStream.range(0, totalPages / chunkSize + 1)
            .mapToObj(i -> new int[]{
                i * chunkSize + 1,
                Math.min((i + 1) * chunkSize, totalPages)
            })
            .toList();

        for (int[] chunk : chunks) {
            CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
                try {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSuppressDuplicateOverlappingText(true);
                    stripper.setStartPage(chunk[0]);
                    stripper.setEndPage(chunk[1]);
                    return stripper.getText(document);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, pool);
            futures.add(cf);
        }

        // 3️⃣ Combine all chunks asynchronously
        CompletableFuture<String> allTextFuture = CompletableFuture
            .allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
//                .collect(Collectors.joining("\n"))
                .collect(Collectors.joining(""))
            );

        // 4️⃣ When complete, write to file and compare
        allTextFuture.thenAccept(combinedText -> {
            try {
                // Write to file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()))) {
                    writer.write(combinedText);
                }

                System.out.println("✅ PDF text extraction complete.");
                System.out.println("Saved to: " + outputPath.toAbsolutePath());
                System.out.println("Text length: " + combinedText.length());

                // Compare with expected
                if (Files.exists(expectedPath)) {
                    byte[] actualBytes = Files.readAllBytes(outputPath);
                    byte[] expectedBytes = Files.readAllBytes(expectedPath);
                    boolean equal = java.util.Arrays.equals(actualBytes, expectedBytes);
                    System.out.println(equal
                        ? "✅ extracted_text.txt matches expected_extracted_text.txt"
                        : "❌ extracted_text.txt differs from expected_extracted_text.txt");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).join(); // Wait until everything finishes

        document.close();
        pool.shutdown();
    }
}
