package com.example.concurency.pdfreader;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RxJavaPdfReaderDemo {

    public static Completable run(String localPdfPath) {
        return Completable.create(emitter -> {
            PDDocument document = PDDocument.load(new File(localPdfPath));

            int totalPages = document.getNumberOfPages();
            int chunkSize = Math.max(10, totalPages / Runtime.getRuntime().availableProcessors());

            // Create an observable stream of page ranges
            List<int[]> chunks = new ArrayList<>();
            for (int start = 1; start <= totalPages; start += chunkSize) {
                int end = Math.min(start + chunkSize - 1, totalPages);
                chunks.add(new int[]{start, end});
            }

            AtomicInteger counter = new AtomicInteger(0);

            Observable.fromIterable(chunks)
                .flatMap(chunk ->
                    Observable.fromCallable(() -> {
                            int start = chunk[0];
                            int end = chunk[1];
                            PDFTextStripper stripper = new PDFTextStripper();
                            stripper.setStartPage(start);
                            stripper.setEndPage(end);
                            String text = stripper.getText(document);
                            int index = counter.getAndIncrement();
                            return Map.entry(index, text);
                        })
                        .subscribeOn(Schedulers.io())
                )
                .toList()
                .map(list -> {
                    // Combine all results in page order
                    list.sort(Comparator.comparingInt(Map.Entry::getKey));
                    String combined = list.stream()
                        .map(Map.Entry::getValue)
                        .reduce("", (a, b) -> a + b);
                    return combined;
                })
                .doFinally(() -> {
                    try {
                        document.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .subscribe(combinedText -> {
                    try {
                        Path outputPath = Path.of("extracted_text.txt");
                        Files.deleteIfExists(outputPath);
                        Files.writeString(outputPath, combinedText, StandardOpenOption.CREATE);
                        System.out.println("PDF text saved to " + outputPath);
                        System.out.println("Total text length: " + combinedText.length());
                        System.out.println("First 400 chars:\n" +
                            combinedText.substring(0, Math.min(400, combinedText.length())));
                        emitter.onComplete();
                    } catch (IOException e) {
                        emitter.onError(e);
                    }
                }, emitter::onError);
        });
    }
}
