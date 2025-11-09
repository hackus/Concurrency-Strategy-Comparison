package com.example.concurency.pdfreader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FuturePdfReaderDemo {

    public static CompletableFuture<Object> run(String localPdfPath) throws Exception {

        final int THREADS = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        PDDocument document = PDDocument.load(new File(localPdfPath));

        int totalPages = document.getNumberOfPages();
        int chunkSize = 200;

        // Process pages in parallel
        List<Future<String>> futures = new ArrayList<>();

        for (int start = 1; start <= totalPages; start += chunkSize) {
            int end = Math.min(start + chunkSize - 1, totalPages);
            int finalStart = start;
            Future<String> future = pool.submit(() -> {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSuppressDuplicateOverlappingText(true);
                stripper.setStartPage(finalStart);
                stripper.setEndPage(end);
                return stripper.getText(document);
            });
            futures.add(future);
        }

        // Combine all extracted text
        StringBuilder combinedText = new StringBuilder();
        for (Future<String> f : futures) {
            combinedText.append(f.get());
        }

        document.close();
        pool.shutdown();

        // Save text to file
        Path outputPath = Path.of("extracted_text.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()))) {
            writer.write(combinedText.toString());
        }

        System.out.println("✅ PDF text extraction complete.");
        System.out.println("Saved to: " + outputPath.toAbsolutePath());
        System.out.println("File size: " + Files.size(outputPath) + " bytes");
        System.out.println("Total text length: " + combinedText.length());
        System.out.println("First 400 chars:\n" +
            combinedText.substring(0, Math.min(400, combinedText.length())));
        return null;
    }
}
