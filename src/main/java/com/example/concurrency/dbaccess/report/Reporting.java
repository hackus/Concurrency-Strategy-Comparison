package com.example.concurrency.dbaccess.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.concurrency.dbaccess.report.ReportHelper.computeDeltas;

@Slf4j
@RequiredArgsConstructor
public class Reporting {
    private static ExtentReports extent;

    private static void startExtentReports() {
        if (extent == null) {
            synchronized (Reporting.class) {
                if (extent == null) {
                    ExtentSparkReporter spark = new ExtentSparkReporter("reports/db/performance-report.html");
                    extent = new ExtentReports();
                    extent.attachReporter(spark);
                }
            }
        }
    }

//    private final ExtentReports extent = new ExtentReports();
    private final Map<String, ExtentTest> testMap = new HashMap<>();

    public void startReport() {
        startExtentReports();
//        ExtentSparkReporter reporter = new ExtentSparkReporter("build/reports/performance-report.html");
//        extent.attachReporter(reporter);
    }

    public void endReport() {
        extent.flush();
    }

    // add this overload (keep your old generateReport(...) if needed elsewhere)
    public void startTest(String normalizedTestName, String testName) {
        testMap.put(normalizedTestName, extent.createTest(testName));
    }

    public void writeReport(
        String testName,
        Map<String, reactor.util.function.Tuple2<Long, Long>> summary,
        Map<String, java.util.List<Long>> timeSeries,
        Map<String, List<Double>> timeSeriesSystemLoad,
        CombinedLatencySeries combinedLatencySeries
        ) {

        // 1) Keep your existing summary bar chart page as a separate HTML
        String summaryHtml = buildBarChartHtml(
            summary.get("insert").getT1(), summary.get("update").getT1(), summary.get("select").getT1(),
            summary.get("insert").getT2(), summary.get("update").getT2(), summary.get("select").getT2()
        );
        String timeHtml = buildTimeSeriesHtml(timeSeries);
        String rateHtml = buildRateSeriesHtml(timeSeries);
        String systemLoadHtml = buildSystemLoadTimeSeriesChartHtml(timeSeriesSystemLoad);
        String latencyChart = buildCombinedLatencyChartHtml(combinedLatencySeries);

        String performanceChartName = "performance-chart-" + testName + ".html";
        String performanceTimelineChartName = "performance-timeline-" + testName + ".html";
        String performanceRateChartName = "performance-rate-" + testName + ".html";
        String performanceSystemLoadChartName = "performance-system-load-" + testName + ".html";
        String performanceLatencyChartName = "performance-latency" + testName + ".html";

        // Write both files
        java.nio.file.Path dir = java.nio.file.Path.of("reports/db");
        try {
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(dir.resolve(performanceChartName), summaryHtml, java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Files.writeString(dir.resolve(performanceTimelineChartName), timeHtml, java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Files.writeString(dir.resolve(performanceRateChartName), rateHtml, java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Files.writeString(dir.resolve(performanceSystemLoadChartName), systemLoadHtml, java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Files.writeString(dir.resolve(performanceLatencyChartName), latencyChart, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed writing chart HTML", e);
        }

        String links = """
            <div id="chartLinks_${testName}" style="margin-bottom:8px;">
              <a href="#" onclick="replaceChart_${testName}('${chart1}', this)">Summary</a>
              <a href="#" onclick="replaceChart_${testName}('${chart2}', this)">Timeline</a>
              <a href="#" onclick="replaceChart_${testName}('${chart3}', this)">Rate</a>
              <a href="#" onclick="replaceChart_${testName}('${chart4}', this)">System Load</a>
              <a href="#" onclick="replaceChart_${testName}('${chart5}', this)">Latency</a>
            </div>
        
            <div id="chartContainer_${testName}">
              <iframe id="chartFrame_${testName}" src="${chart1}" width="900" height="520"
                      style="border:1px solid #ccc;border-radius:6px;margin-top:12px"></iframe>
            </div>
        
            <script>
              function replaceChart_${testName}(page, el) {
                document.querySelectorAll('#chartLinks_${testName} a').forEach(a => a.classList.remove('active'));
                el.classList.add('active');
                const iframe = document.createElement('iframe');
                iframe.width = 900;
                iframe.height = 520;
                iframe.style.border = '1px solid #ccc';
                iframe.style.borderRadius = '6px';
                iframe.style.marginTop = '12px';
                iframe.src = page;
                const container = document.getElementById('chartContainer_${testName}');
                container.innerHTML = '';
                container.appendChild(iframe);
                const testItem = document.querySelector('li.test-item.active');
                if (testItem) testItem.click();
                return false;
              }
            </script>
        
            <style>
              #chartLinks_${testName} a {
                text-decoration:none;
                margin-right:12px;
                color:#007bff;
                cursor:pointer;
              }
              #chartLinks_${testName} a.active {
                font-weight:bold;
                color:#000;
              }
            </style>
            """;

        links = links.replace("${testName}", testName)
            .replace("${chart1}", performanceChartName)
            .replace("${chart2}", performanceTimelineChartName)
            .replace("${chart3}", performanceRateChartName)
            .replace("${chart4}", performanceSystemLoadChartName)
            .replace("${chart5}", performanceLatencyChartName);

        var test = testMap.get(testName);
        test.info(links);
//        extent.flush();
    }

    // === helpers ===

    private String buildCombinedLatencyChartHtml(CombinedLatencySeries combined) {
        CombinedLatencySeries.CombinedLatencies combinedLatencies = combined.getCombinedLatencies();

        String labels = combinedLatencies.seconds().stream()
            .map(s -> "\"" + s + "s\"")
            .collect(Collectors.joining(","));

        String insertStr = combinedLatencies.insert().stream()
            .map(v -> String.format(Locale.US, "%.3f", v))
            .collect(Collectors.joining(","));
        String updateStr = combinedLatencies.update().stream()
            .map(v -> String.format(Locale.US, "%.3f", v))
            .collect(Collectors.joining(","));
        String selectStr = combinedLatencies.select().stream()
            .map(v -> String.format(Locale.US, "%.3f", v))
            .collect(Collectors.joining(","));

        return String.format("""
    <!doctype html>
    <html>
    <head>
      <meta charset="utf-8"/>
      <title>Combined Operation Latency</title>
      <style>
        body { font-family: Arial, sans-serif; padding: 16px; }
        .container { max-width: 980px; margin: 0 auto; }
      </style>
    </head>
    <body>
      <div class="container">
        <h2>Average Operation Latency (ms) per Second</h2>
        <canvas id="latencyChart" width="900" height="420"></canvas>
      </div>

      <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
      <script>
        const ctx = document.getElementById('latencyChart').getContext('2d');
        new Chart(ctx, {
          type: 'line',
          data: {
            labels: [%s],
            datasets: [
              {
                label: 'Insert Latency (ms)',
                data: [%s],
                borderColor: 'rgba(54,162,235,1)',
                backgroundColor: 'rgba(54,162,235,0.2)',
                fill: false,
                tension: 0.2
              },
              {
                label: 'Update Latency (ms)',
                data: [%s],
                borderColor: 'rgba(255,206,86,1)',
                backgroundColor: 'rgba(255,206,86,0.2)',
                fill: false,
                tension: 0.2
              },
              {
                label: 'Select Latency (ms)',
                data: [%s],
                borderColor: 'rgba(75,192,192,1)',
                backgroundColor: 'rgba(75,192,192,0.2)',
                fill: false,
                tension: 0.2
              }
            ]
          },
          options: {
            responsive: true,
            plugins: {
              title: {
                display: true,
                text: 'Average Latency per Operation'
              },
              tooltip: {
                mode: 'index',
                intersect: false,
                callbacks: {
                  label: item => `${item.dataset.label}: ${item.formattedValue} ms`
                }
              },
              legend: { position: 'top' }
            },
            scales: {
              y: { beginAtZero: true, title: { display: true, text: 'Latency (ms)' } },
              x: { title: { display: true, text: 'Time (seconds)' } }
            }
          }
        });
      </script>
    </body>
    </html>
    """, labels, insertStr, updateStr, selectStr);
    }

    private String buildSystemLoadTimeSeriesChartHtml(Map<String, List<Double>> timeSeriesSystemLoad) {
        List<Double> systemCpu = timeSeriesSystemLoad.getOrDefault("systemCpuUsage", List.of());
        List<Double> jvmCpu    = timeSeriesSystemLoad.getOrDefault("jvmCpuUsage", List.of());
        List<Double> memory    = timeSeriesSystemLoad.getOrDefault("memoryLoad", List.of());

        String timeLabels = IntStream.range(0, systemCpu.size())
            .mapToObj(i -> "\"" + i + "s\"")
            .collect(Collectors.joining(","));
        String systemStr = systemCpu.stream().map(v -> String.format(Locale.US, "%.2f", v)).collect(Collectors.joining(","));
        String jvmStr    = jvmCpu.stream().map(v -> String.format(Locale.US, "%.2f", v)).collect(Collectors.joining(","));
        String memStr    = memory.stream().map(v -> String.format(Locale.US, "%.2f", v)).collect(Collectors.joining(","));

        return String.format("""
        <!doctype html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <title>System Load Time Series</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 16px; }
            .container { max-width: 980px; margin: 0 auto; }
          </style>
        </head>
        <body>
          <div class="container">
            <h2>System / JVM CPU and Memory Load Over Time</h2>
            <canvas id="loadChart" width="900" height="400"></canvas>
          </div>

          <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
          <script>
            const ctx = document.getElementById('loadChart').getContext('2d');
            new Chart(ctx, {
              type: 'line',
              data: {
                labels: [%s],
                datasets: [
                  {
                    label: 'System CPU (%%%%)',
                    data: [%s],
                    borderColor: 'orange',
                    backgroundColor: 'rgba(255,165,0,0.3)',
                    fill: false,
                    tension: 0.2
                  },
                  {
                    label: 'JVM CPU (%%%%)',
                    data: [%s],
                    borderColor: 'red',
                    backgroundColor: 'rgba(255,0,0,0.3)',
                    fill: false,
                    tension: 0.2
                  },
                  {
                    label: 'Memory Load (%%%%)',
                    data: [%s],
                    borderColor: 'green',
                    backgroundColor: 'rgba(0,128,0,0.3)',
                    fill: false,
                    tension: 0.2
                  }
                ]
              },
              options: {
                responsive: true,
                plugins: {
                  title: { display: true, text: 'System Load (%%%%) Over Time' },
                  legend: { position: 'top' },
                  tooltip: {
                    mode: 'index',
                    intersect: false,
                    callbacks: {
                      title: items => `Time: ${items[0].label}`,
                      label: item => `${item.dataset.label}: ${item.formattedValue}%%%%`
                    }
                  }
                },
                scales: {
                  y: { beginAtZero: true, max: 100 },
                  x: { title: { display: true, text: 'Time (seconds)' } }
                }
              }
            });
          </script>
        </body>
        </html>
        """, timeLabels, systemStr, jvmStr, memStr);
    }

    private String buildBarChartHtml(
        long insertOk, long updateOk, long selectOk,
        long insertFail, long updateFail, long selectFail) {

        return """
        <!doctype html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <title>DB Performance Summary</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 16px; }
            .container { max-width: 980px; margin: 0 auto; }
          </style>
        </head>
        <body>
          <div class="container">
            <h2>DB Operation Success / Fail Count</h2>
            <canvas id="bar" width="900" height="380"></canvas>
          </div>

          <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
          <script>
            const ctx = document.getElementById('bar').getContext('2d');
            const chart = new Chart(ctx, {
              type: 'bar',
              data: {
                labels: ['INSERT', 'UPDATE', 'SELECT'],
                datasets: [
                  {
                    label: 'Succeeded',
                    data: [%d, %d, %d],
                    backgroundColor: 'rgba(54, 162, 235, 0.7)',
                    borderColor: 'rgba(54, 162, 235, 1)',
                    borderWidth: 1
                  },
                  {
                    label: 'Failed',
                    data: [%d, %d, %d],
                    backgroundColor: 'rgba(255, 99, 132, 0.7)',
                    borderColor: 'rgba(255, 99, 132, 1)',
                    borderWidth: 1
                  }
                ]
              },
              options: {
                responsive: true,
                plugins: {
                  title: {
                    display: true,
                    text: 'DB Operation Summary'
                  },
                  tooltip: {
                    mode: 'index',       // show all datasets for same index
                    intersect: false,    // hover anywhere near the group
                    callbacks: {
                      title: (items) => items[0].label,
                      label: (item) => {
                        const label = item.dataset.label;
                        const value = item.formattedValue;
                        const symbol = label === 'Succeeded' ? '✓' : '✗';
                        return `${symbol} ${label}: ${value}`;
                      }
                    }
                  },
                  legend: { position: 'top' }
                },
                scales: {
                  x: { stacked: false },
                  y: { beginAtZero: true }
                }
              }
            });
          </script>
        </body>
        </html>
        """.formatted(
            insertOk, updateOk, selectOk,
            insertFail, updateFail, selectFail
        );
    }

    private static String toJsArray(java.util.List<Long> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    private String buildTimeSeriesHtml(Map<String, java.util.List<Long>> ts) {
        // labels 0..N-1 (seconds)
        int n = 0;
        for (var e : ts.entrySet()) n = Math.max(n, e.getValue().size());
        StringBuilder labels = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) labels.append(',');
            labels.append(i);
        }
        labels.append(']');

        String insertSucc = toJsArray(ts.get("insert_succ"));
        String insertFail = toJsArray(ts.get("insert_fail"));
        String updateSucc = toJsArray(ts.get("update_succ"));
        String updateFail = toJsArray(ts.get("update_fail"));
        String selectSucc = toJsArray(ts.get("select_succ"));
        String selectFail = toJsArray(ts.get("select_fail"));

        return """
            <!doctype html>
            <html><head>
              <meta charset="utf-8"/>
              <title>DB Performance Timeline</title>
              <style>body{font-family:Arial;padding:16px}.container{max-width:1100px;margin:0 auto}</style>
            </head><body>
              <div class="container">
                <h2>Cumulative Success/Fail Over Time (per second)</h2>
                <canvas id="line" width="1000" height="420"></canvas>
              </div>
              <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
              <script>
                const labels = %s;
            
                const data = {
                  labels,
                  datasets: [
                    { label: 'Insert - Success', data: %s, fill:false, tension:0.1 },
                    { label: 'Insert - Fail',    data: %s, fill:false, tension:0.1 },
                    { label: 'Update - Success', data: %s, fill:false, tension:0.1 },
                    { label: 'Update - Fail',    data: %s, fill:false, tension:0.1 },
                    { label: 'Select - Success', data: %s, fill:false, tension:0.1 },
                    { label: 'Select - Fail',    data: %s, fill:false, tension:0.1 }
                  ]
                };
            
                new Chart(document.getElementById('line').getContext('2d'), {
                  type: 'line',
                  data,
                  options: {
                    responsive: true,
                    interaction: { mode: 'index', intersect: false },
                    stacked: false,
                    plugins: { title: { display: true, text: 'Cumulative Results By Second' } },
                    scales: { y: { beginAtZero: true } }
                  }
                });
              </script>
            </body></html>
            """.formatted(labels, insertSucc, insertFail, updateSucc, updateFail, selectSucc, selectFail);
    }

    private String buildRateSeriesHtml(Map<String, List<Long>> ts) {
        // compute per-second deltas
        List<Long> insertSucc = computeDeltas(ts.get("insert_succ"));
        List<Long> insertFail = computeDeltas(ts.get("insert_fail"));
        List<Long> updateSucc = computeDeltas(ts.get("update_succ"));
        List<Long> updateFail = computeDeltas(ts.get("update_fail"));
        List<Long> selectSucc = computeDeltas(ts.get("select_succ"));
        List<Long> selectFail = computeDeltas(ts.get("select_fail"));

        int n = Math.max(insertSucc.size(),
            Math.max(updateSucc.size(), selectSucc.size()));
        StringBuilder labels = new StringBuilder("[");
        for (int i = 0; i < n; i++) { if (i > 0) labels.append(','); labels.append(i); }
        labels.append(']');

        return """
        <!doctype html>
        <html><head>
          <meta charset="utf-8"/>
          <title>DB Operation Rates per Second</title>
          <style>body{font-family:Arial;padding:16px}.container{max-width:1100px;margin:0 auto}</style>
        </head><body>
          <div class="container">
            <h2>Operation Success/Failure Rate per Second</h2>
            <canvas id="rateChart" width="1000" height="420"></canvas>
          </div>
          <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
          <script>
            const labels = %s;
            new Chart(document.getElementById('rateChart').getContext('2d'), {
              type: 'line',
              data: {
                labels,
                datasets: [
                  { label:'Insert-Success', data:%s, borderColor:'green', fill:false },
                  { label:'Insert-Fail',    data:%s, borderColor:'darkred', fill:false },
                  { label:'Update-Success', data:%s, borderColor:'blue', fill:false },
                  { label:'Update-Fail',    data:%s, borderColor:'red', fill:false },
                  { label:'Select-Success', data:%s, borderColor:'orange', fill:false },
                  { label:'Select-Fail',    data:%s, borderColor:'brown', fill:false }
                ]
              },
              options:{
                responsive:true,
                interaction:{mode:'index',intersect:false},
                plugins:{title:{display:true,text:'DB Ops per-Second Rate'}},
                scales:{y:{beginAtZero:true,title:{display:true,text:'Ops/sec'}}}
              }
            });
          </script>
        </body></html>
        """.formatted(labels,
            toJsArray(insertSucc), toJsArray(insertFail),
            toJsArray(updateSucc), toJsArray(updateFail),
            toJsArray(selectSucc), toJsArray(selectFail));
    }

    public void fail(String testName, String message) {
        var test = testMap.get(testName);
        test.fail(message);
//        extent.flush();
    }

    public void pass(String testName, String message) {
        var test = testMap.get(testName);
        test.pass(message);
//        extent.flush();
    }
}
