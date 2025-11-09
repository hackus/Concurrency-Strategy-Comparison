package com.example.concurency.pdfreader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class VirtualThreadPdfReaderDemo {

    public static void run(String localPdfPath) throws Exception {
        Queue<Map.Entry<Integer, String>> results;
        PDDocument document = PDDocument.load(new File(localPdfPath));

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {


            int totalPages = document.getNumberOfPages();
            int chunkSize = Math.max(10, totalPages / Runtime.getRuntime().availableProcessors());

            // Thread-safe collector for results
            results = new ConcurrentLinkedQueue<>();

            for (int start = 1; start <= totalPages; start += chunkSize) {
                int end = Math.min(start + chunkSize - 1, totalPages);
                int finalStart = start;

                pool.execute(() -> {
                    try {
                        PDFTextStripper stripper = new PDFTextStripper();
                        stripper.setSuppressDuplicateOverlappingText(true);
                        stripper.setStartPage(finalStart);
                        stripper.setEndPage(end);

                        String text = stripper.getText(document);
                        results.add(Map.entry(finalStart, text));

                        System.out.printf("✅ Extracted pages %d–%d on %s%n",
                            finalStart, end, Thread.currentThread());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            // 🔒 Close waits for all virtual threads to finish
        }

        document.close();

        List<Map.Entry<Integer, String>> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparingInt(Map.Entry::getKey));

        String combinedText = sorted.stream()
            .map(Map.Entry::getValue)
            .collect(Collectors.joining(""));

        Path outputPath = Path.of("extracted_text.txt");
        Files.deleteIfExists(outputPath);
        Files.writeString(outputPath, combinedText, StandardOpenOption.CREATE);

        System.out.println("✅ Extracted text saved to " + outputPath);
        System.out.println("Total text length: " + combinedText.length());
        System.out.println("First 400 chars:\n" + combinedText.substring(0, Math.min(400, combinedText.length())));
    }
}
