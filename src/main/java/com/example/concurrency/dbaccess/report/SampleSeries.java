package com.example.concurrency.dbaccess.report;

import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SampleSeries {

    final long benchmarkStartNs;
    // key = second timestamp, value = list of durations (ns)
    private final ConcurrentMap<Long, List<Long>> samples = new ConcurrentHashMap<>();

    /**
     * Add a new sample.
     * @param sample
     * sample.startTimeNs start time in nanoseconds
     * sample.durationNs  duration in nanoseconds
     */
    public void addSample(SampleEntry sample) {
        long relativeSec = (sample.startTimeNs - benchmarkStartNs) / 1_000_000_000L;
        samples.computeIfAbsent(relativeSec, k -> new CopyOnWriteArrayList<>())
            .add(sample.durationNs);
    }

    /**
     * Returns all samples grouped by start second.
     */
    public Map<Long, List<Long>> groupedBySecond() {
        return Collections.unmodifiableMap(samples);
    }

    /**
     * Returns average duration per second, in milliseconds.
     */
    public Map<Long, Double> averageLatencyBySecondMs() {
        return samples.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream()
                    .mapToDouble(ns -> ns / 1_000_000.0)
                    .average()
                    .orElse(0.0)
            ));
    }

    /**
     * Returns the averages as a sorted list of seconds -> avgLatencyMs.
     */
    public List<Map.Entry<Long, Double>> sortedAverages() {
        return averageLatencyBySecondMs().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .toList();
    }
}

