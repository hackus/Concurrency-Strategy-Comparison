package com.example.concurency.pdfreader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CompletableFuturePdfReaderNonBlockingDemo {

    public static CompletableFuture<Object> run(String pdfPath) {
        final int THREADS = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        PDDocument doc;
        try {
            doc =  PDDocument.load(new File(pdfPath));
        } catch (IOException e) {
            throw new CompletionException(e);
        }

        var result = CompletableFuture.supplyAsync(() -> doc, pool).thenComposeAsync(document -> {
            int totalPages = document.getNumberOfPages();
            int chunkSize = Math.max(10, totalPages / THREADS);

            List<CompletableFuture<Map.Entry<Integer, String>>> futures = new ArrayList<>();

            for (int start = 1; start <= totalPages; start += chunkSize) {
                int end = Math.min(start + chunkSize - 1, totalPages);
                int finalStart = start;

                CompletableFuture<Map.Entry<Integer, String>> future =
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            PDFTextStripper stripper = new PDFTextStripper();
                            stripper.setSuppressDuplicateOverlappingText(true);
                            stripper.setStartPage(finalStart);
                            stripper.setEndPage(end);
                            String text = stripper.getText(document);
                            return Map.entry(finalStart, text);
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        }
                    }, pool);

                futures.add(future);
            }

            // Wait for all tasks and combine results IN PAGE ORDER (submission order)
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .sorted(Comparator.comparingInt(Map.Entry::getKey))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.joining("")))
                .whenComplete((text, ex) -> {
                    try {
                        document.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        }, pool).thenComposeAsync(combinedText -> {

            Path outputPath = Path.of("extracted_text.txt");

            try {
                Files.deleteIfExists(outputPath);
                Files.writeString(outputPath, combinedText, StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new CompletionException(e);
            }

            System.out.println("✅ PDF extracted successfully to " + outputPath);
            System.out.println("Total text length: " + combinedText.length());
            System.out.println("First 400 chars:\n" +
                combinedText.substring(0, Math.min(400, combinedText.length())));

            return CompletableFuture.completedFuture(null);

        }, pool).whenComplete((r, ex) -> pool.shutdown());

//        try {
//            doc.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return result;
    }
}
