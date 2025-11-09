package com.example.concurency.dbaccess.report;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Slf4j
public class CombinedLatencySeries {

    public final SampleSeries insertHolder;
    public final SampleSeries updateHolder;
    public final SampleSeries selectHolder;

    public CombinedLatencySeries(SampleSeries insertHolder,
                                 SampleSeries updateHolder,
                                 SampleSeries selectHolder) {

        this.insertHolder = insertHolder;
        this.updateHolder = updateHolder;
        this.selectHolder = selectHolder;
    }

    public CombinedLatencies getCombinedLatencies(){
        Map<Long, Double> insertMap = insertHolder.averageLatencyBySecondMs();
        Map<Long, Double> updateMap = updateHolder.averageLatencyBySecondMs();
        Map<Long, Double> selectMap = selectHolder.averageLatencyBySecondMs();

        // 1. Build a unified sorted set of all second keys
        Set<Long> allSeconds = new TreeSet<>();
        allSeconds.addAll(insertMap.keySet());
        allSeconds.addAll(updateMap.keySet());
        allSeconds.addAll(selectMap.keySet());

        // 2. Fill aligned lists
        List<Long> seconds = new ArrayList<>(allSeconds);
        List<Double> insert = new ArrayList<>();
        List<Double> update = new ArrayList<>();
        List<Double> select = new ArrayList<>();

        for (Long sec : seconds) {
            insert.add(insertMap.getOrDefault(sec, 0.0));
            update.add(updateMap.getOrDefault(sec, 0.0));
            select.add(selectMap.getOrDefault(sec, 0.0));
        }

        return new CombinedLatencies(seconds, insert, update, select);
    }

    public record CombinedLatencies(List<Long> seconds, List<Double> insert, List<Double> update, List<Double> select) {
    }

    public record CombinedAvgLatencies(double totalAvg, double insertAvg, double updateAvg, double selectAvg) {
    }

    private double averageLatencyMs(List<Double> samples) {
        long count = samples.size();
        double totalNs = samples.stream()
            .mapToDouble(item -> item)
            .sum();

        return count > 0 ? (totalNs / count) : 0.0;
    }

    public CombinedAvgLatencies getTotalAverageLatency(){
        double insertAvg = averageLatencyMs(insertHolder.averageLatencyBySecondMs().values().stream().toList());
        double updateAvg = averageLatencyMs(updateHolder.averageLatencyBySecondMs().values().stream().toList());
        double selectAvg = averageLatencyMs(selectHolder.averageLatencyBySecondMs().values().stream().toList());

        // Combine all latencies together for global average:
        List<SampleSeries> allHolders = List.of(
            insertHolder,
            updateHolder,
            selectHolder
        );

        double totalSumNs = 0.0;
        long totalCount = 0;

        for (SampleSeries holder : allHolders) {
            for (List<Long> durations : holder.groupedBySecond().values()) {
                for (Long ns : durations) {
                    totalSumNs += ns;
                    totalCount++;
                }
            }
        }

        double totalAvgMs = totalCount > 0 ? (totalSumNs / totalCount) / 1_000_000.0 : 0.0;

        log.info("Insert avg = {} ms, Update avg = {} ms, Select avg = {} ms, Total avg = {} ms",
            insertAvg, updateAvg, selectAvg, totalAvgMs);

        return new CombinedAvgLatencies(totalAvgMs, insertAvg, updateAvg, selectAvg);
    }
}
