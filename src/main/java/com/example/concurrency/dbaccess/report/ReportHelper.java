package com.example.concurrency.dbaccess.report;

import java.util.ArrayList;
import java.util.List;

public class ReportHelper {
    static List<Long> computeDeltas(List<Long> cumulative) {
        List<Long> deltas = new ArrayList<>();
        if (cumulative == null || cumulative.isEmpty()) return deltas;
        deltas.add(cumulative.get(0));
        for (int i = 1; i < cumulative.size(); i++) {
            deltas.add(cumulative.get(i) - cumulative.get(i - 1));
        }
        return deltas;
    }
}
