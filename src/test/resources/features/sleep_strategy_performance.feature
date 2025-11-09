Feature: Compare thread sleep concurrency strategies

  Scenario Outline: Run performance with hard sleep strategy and <strategy>
    When I run the <mode> sleep "<strategy>"
    Then the run should complete within <timeout(ms)> ms

    Examples:
      | mode | strategy                                        | timeout(ms) |
      | hard | FutureCommonPoolDemo                            | 2000        |
      | hard | FutureExecutorServiceDemo                       | 2000        |
      | hard | FutureExecutorService1000ThreadsDemo            | 2000        |
      | hard | CompletableFutureDefaultDemo                    | 2000        |
      | hard | CompletableFutureExecutorServiceThreadsDemo     | 2000        |
      | hard | CompletableFutureExecutorService1000ThreadsDemo | 2000        |
      | hard | VirtualThreadDemo                               | 2000        |
      | hard | ReactiveReactorDemo                             | 2000        |
      | hard | ReactiveReactorParallelDemo                     | 2000        |
      | hard | ReactiveRxJavaDemo                              | 2000        |
      | hard | ReactiveRxJavaParallelDemo                      | 2000        |

  Scenario Outline: Run performance with soft sleep strategy and <strategy>
    When I run the <mode> sleep "<strategy>"
    Then the run should complete within <timeout(ms)> ms

    Examples:
      | mode | strategy                                        | timeout(ms) |
      | hard | FutureCommonPoolDemo                            | 2000        |
      | hard | FutureExecutorServiceDemo                       | 2000        |
      | hard | FutureExecutorService1000ThreadsDemo            | 2000        |
      | hard | CompletableFutureDefaultDemo                    | 2000        |
      | hard | CompletableFutureExecutorServiceThreadsDemo     | 2000        |
      | hard | CompletableFutureExecutorService1000ThreadsDemo | 2000        |
      | hard | VirtualThreadDemo                               | 2000        |
      | hard | ReactiveReactorDemo                             | 2000        |
      | hard | ReactiveReactorParallelDemo                     | 2000        |
      | hard | ReactiveRxJavaDemo                              | 2000        |
      | hard | ReactiveRxJavaParallelDemo                      | 2000        |
