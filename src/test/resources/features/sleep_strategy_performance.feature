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
      | soft | FutureCommonPoolDemo                            | 2000        |
      | soft | FutureExecutorServiceDemo                       | 2000        |
      | soft | FutureExecutorService1000ThreadsDemo            | 2000        |
      | soft | CompletableFutureDefaultDemo                    | 2000        |
      | soft | CompletableFutureExecutorServiceThreadsDemo     | 2000        |
      | soft | CompletableFutureExecutorService1000ThreadsDemo | 2000        |
      | soft | VirtualThreadDemo                               | 2000        |
      | soft | ReactiveReactorDemo                             | 2000        |
      | soft | ReactiveReactorParallelDemo                     | 2000        |
      | soft | ReactiveRxJavaDemo                              | 2000        |
      | soft | ReactiveRxJavaParallelDemo                      | 2000        |
