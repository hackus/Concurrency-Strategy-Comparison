
## For a better experience and to view the charts read the web page version

[Web page →](https://hackus.github.io/Concurrency-Strategy-Comparison/)

If it opens the same page it means you are already on it. 

Note: The reports may change over time. When I make updates, I’ll rerun and repost them.

## 🧠 Introduction

I started this project to gain a clear, firsthand understanding of how different concurrency solutions compare. Recently, virtual threads have been gaining traction, and my
interest in them grew as I began exploring reactive frameworks. However, I’ve noticed that many discussions on the topic seem to lack practical insight — people often repeat what
they’ve read or heard without ever experimenting with these technologies themselves. I see this as a poor practice that I wanted to avoid.

There’s also an architectural aspect to this exploration. I want to understand which concurrency model is best suited for different scenarios. Although the primary goal isn’t to
uncover architectural advantages directly, such insights can be inferred by analyzing the charts and identifying patterns. If a task shows similar characteristics, the most optimal
solution can then be chosen based on the gathered statistics.

## ✨ This project explores and benchmarks five major concurrency paradigms:

### Future — the classic Java 5 async abstraction.

### CompletableFuture — fluent, composable async tasks (Java 8).

### Virtual Threads — lightweight threads enabling structured concurrency (Java 21).

### Reactive RxJava — event-driven asynchronous programming via observables.

### Reactive Reactor — the foundation of Spring WebFlux and modern reactive microservices.

Each model runs identical workloads — such as database I/O, PDF parsing, and simulated thread-sleep tasks — to compare performance, scalability, and development complexity.
The goal is to understand trade-offs between simplicity, control, and scalability — and to help developers choose the right concurrency tool for their workload.

## 🚀 Analysis

At first, I believed virtual threads could serve as a strong alternative to reactive programming—especially after running a simulation with Thread.sleep and observing how virtual threads responded instantly as each sleep interval completed.

But then I realized something unusual — it wasn’t really meaningful to schedule multiple threads just to run Thread.sleep simultaneously. That test didn’t demonstrate much beyond the fact that virtual threads are lightweight. So, I began exploring more practical scenarios to truly test their behavior. I decided to make each thread perform a real task, such as reading a file, since virtual threads are often recommended for I/O-bound operations. This experiment revealed that, for such workloads, well-tuned Future or CompletableFuture implementations can achieve comparable performance.

[Task simulation using hard sleep →](reports/sleep_strategy/Run_performance_with_hard_sleep_strategy.html)

[Task simulation using soft sleep →](reports/sleep_strategy/Run_performance_with_soft_sleep_strategy.html)

Next, I took a different approach — I wanted to simulate communication with a third-party service, and I chose an H2 database for this purpose. That’s when I discovered that virtual threads can become problematic in such scenarios. They can put significant pressure both on the host machine and on the external service they interact with. Inevitably, some form of backpressure or throttling mechanism is required. This turned out to be a key insight: it highlights that virtual threads occupy a specific niche rather than being a universal solution, and it also reinforces why reactive frameworks remain indispensable.

[VirtualThreads pass rate →](reports/db/performance-chart-run_performance_with_VirtualThreads.html)

Another important finding was that CompletableFuture can, in many cases, effectively replace both reactive frameworks and virtual threads. When properly tuned, it delivers the stability typically associated with reactive frameworks and the speed often attributed to virtual threads. From my experiments, I concluded that virtual threads are indeed well-suited for I/O-bound tasks. However, when it comes to third-party communication, the optimal choice depends on the database type and throughput requirements: for relational databases, I would choose CompletableFuture, while for NoSQL systems, reactive frameworks remain the better option.

There is also another opinion I have towards VT. There is this Erlang language that is advertised to be able to spawn "millions" of threads, which I am highly susceptible of.
Spawning empty threads should not be a problem in any language. Naturally, as the number of active threads increases, system performance will degrade. Interestingly, Erlang in 2023 was ranked ~36, and in 2025 is ranked ~48, which seems like a promising trend — just in the wrong direction. I mean, if spawning millions of threads had been such a great idea then statistics would have revealed it. 

[Debug.to top 50 PG 2023 →](https://debug.to/6293/top-50-programming-languages-in-2023)

[Tiobe top PG 2025 →](https://www.tiobe.com/tiobe-index/)

That said, I’m somewhat skeptical of Java’s virtual threads for similar reasons. Still, when used for local computations that don’t involve third-party interactions, virtual threads perform impressively well. This somewhat contrasts with Erlang’s use of the Actor model, which is deeply embedded in the language. Erlang itself is probably not a bad language — its decline in popularity may have more to do with its steep learning curve and functional programming complexity than with its technical capabilities.

As for stability it was interesting to observe how stable the ReactiveRx rate chart is as opposed to CompletableFuture and VirtualThreads rate chart when testing how each solution behaves while communicating with third party services.

[VirtualThreads rate Chart →](reports/db/performance-rate-run_performance_with_VirtualThreads.html)

[CompletableFuture rate chart →](reports/db/performance-rate-run_performance_with_CompletableFuture.html)

[ReactiveRx rate Chart →](reports/db/performance-rate-run_performance_with_ReactiveRxJavaDBManager.html)

The chaotic behavior of virtual threads becomes immediately apparent in the chart. It’s clear that spawning a million virtual threads to communicate with a third-party service is unrealistic — even two thousand already feels excessive. The results also highlight just how exceptional CompletableFuture remains; in many ways, it offers the best overall balance. Meanwhile, reactive frameworks stand out as the safest and most predictable option.

[VirtualThreads pass rate →](reports/db/performance-chart-run_performance_with_VirtualThreads.html)

[CompletableFuture pass rate →](reports/db/performance-chart-run_performance_with_CompletableFuture.html)

[ReactiveRx pass rate →](reports/db/performance-chart-run_performance_with_ReactiveRxJavaDBManager.html)

## 📉 Full report for db access
[DB performance full report →](reports/db/performance-report.html)

## 📉 Pdf reader report
[Pdf reader report →](reports/pdf_reader/pdf_reader_report.html)

## 📉 Sleep strategy
[Task simulator hard sleep report →](reports/sleep_strategy/Run_performance_with_hard_sleep_strategy.html)
[Task simulator soft sleep report →](reports/sleep_strategy/Run_performance_with_soft_sleep_strategy.html)

## Project link
[Here →](https://github.com/hackus/ConcurencyStrategyComparison)


## 🧩 Concurrency Models Compared

| Model                 | Description                                                       | Characteristics                                                                                                    | Ideal Use Case                                    |
|-----------------------|-------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| **Future**            | Introduced in Java 5 as a simple handle for asynchronous results. | - Blocking `get()`<br>- No composition<br>- Minimal control                                                        | Simple async tasks                                |
| **CompletableFuture** | Java 8 extension with fluent composition and chaining.            | - Non-blocking<br>- Custom executors<br>- Functional style                                                         | Parallel pipelines, async composition             |
| **Virtual Threads**   | Java 21 (Project Loom) — lightweight threads managed by JVM.      | - Thousands of threads<br>- Natural blocking style<br>- Minimal boilerplate                                        | Highly concurrent I/O workloads                   |
| **Reactive RxJava**   | Reactive Extensions for Java (library-based).                     | - Push-based async streams<br>- Non-blocking<br>- Fine control over backpressure                                   | Reactive APIs, high-frequency data streams        |
| **Reactive Reactor**  | Core reactive engine from Spring ecosystem.                       | - Non-blocking `Mono` / `Flux` types<br>- Natively integrated in Spring WebFlux<br>- Efficient context propagation | Microservices, reactive APIs, streaming pipelines |

## 📊 Performance Observations

| Metric                          | Future               | CompletableFuture     | Virtual Threads               | RxJava                        | Reactor                       |
|---------------------------------|----------------------|-----------------------|-------------------------------|-------------------------------|-------------------------------|
| **Blocking Tasks**              | ❌ Thread-limited     | ⚠️ Pool-bound         | ✅ Excellent                   | ✅ Excellent                   | ✅ Excellent                   |
| **Composition / Chaining**      | ❌ None               | ✅ Fluent              | ⚠️ Sequential only            | ✅ Powerful                    | ✅ Powerful                    |
| **Backpressure / Flow Control** | ❌                    | ❌                     | ❌                             | ✅ Yes                         | ✅ Yes                         |
| **Ease of Debugging**           | ✅ Simple             | ⚠️ Moderate           | ✅ Familiar                    | ⚠️ Complex                    | ⚠️ Complex                    |
| **Throughput (I/O-bound)**      | Very High/Low/Medium | Very High/High/Medium | Very High/Medium              | Very High/Medium/Low/Very Low | Very High/Medium/Low/Very Low |
| **Thread Management**           | Manual               | Configurable          | Implicit, lightweight         | Managed                       | Managed                       |
| **Best for**                    | Simple async         | Chained async ops     | Blocking-style concurrent I/O | Event-driven data flows       | WebFlux, reactive pipelines   |

## 🧮 Benchmark Summary

| Test                          | Future  | CompletableFuture | Virtual Threads | RxJava   | Reactor  |
|-------------------------------|---------|-------------------|-----------------|----------|----------|
| 🧠 TaskSimulator (1000 tasks) | 1121 ms | 1097 ms           | 1084 ms         | 8291 ms  | 10675 ms |
| 📄 PDF Reader (multi-page)    | 3101 ms | 1509 ms           | 1524 ms         | 1885 ms  | N/A      |
| 🗄️ DB  (HikariCP)            | N/A     | ~25000 ms         | ~24000 ms       | 10000 ms | N/A      |

For the TaskSimulator Future's and CompletableFuture's values are taken from the tuned implementations.
For DB values taken are max between insert, update and delete.

## 🧱 Project Structure

```
reports/
├── db/
│   ├── ...
│   └── performance-report.html
├── pdf_reader/
│   └── pdf_reader_report.html
├── sleep_strategy/
│   ├── Run_performance_with_hard_sleep_strategy.html
│   └── Run_performance_with_soft_sleep_strategy.html
src/main/java/com/example/concurency/
├── pdfreader/
│   └── ... reader implementation
├── threadsleep/
│   └── ... simulate task with thread sleep
├── dbaccess/
│   └── ... third party communication
src/test/java/com/example/concurency/
├── common/
│   ├── StepContext.java     # context dto
│   └── ValidationSteps.java # common validation step
├── dbaccess/
│   ├── DbPerformanceRunner.java # cucumber runner for db tests
│   └── DbPerformanceSteps.java  # cucumber db steps
├── pdfreader/
│   ├── PdfReaderRunner.java # cucumber runner for pdf reader tests
│   └── PdfReaderSteps.java  # cucumber pdf reader steps
├── threadsleep/
│   ├── SleepStrategyComparisonRunner.java # cucumber runner for task simulator tests
│   └── SleepStrategyComparisonSteps.java  # cucumber task simulator steps
src/resources/
├── features/
│   ├── db_performance.feature              # cucumber db feature
│   ├── pdf_reader_performance.feature      # cucumber pdf reader feature
│   └── sleep_strategy_performance.feature  # cucumber task simulator feature
├── combinepdf-1.pdf             # pdf file for pdf reader feature
├── expected_extracted_text.txt  # expected pdf content
└── junit-platform.properties    # configs
```