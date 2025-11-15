@dbtest
Feature: Database performance testing

  Scenario Outline: Run performance test using <strategy> strategy
    Given the H2 database <strategy> is initialized and <report_path> as report path
    When I run performance tests with 2000 concurrent users and <strategy> strategy and <report_path> as report path
    Then the average insert response time should be under <insert_latency> ms and percentage of failed less than <failed_percentage> percent
    And the average update response time should be under <update_latency> ms and percentage of failed less than <failed_percentage> percent
    And the average select response time should be under <select_latency> ms and percentage of failed less than <failed_percentage> percent

    Examples:
      | strategy                | insert_latency | update_latency | select_latency | failed_percentage | report_path         |
      | VirtualThreads          | 200            | 1000           | 700            | 0.02              | "./reports/db"      |
      | CompletableFuture       | 200            | 1000           | 700            | 0.02              | "./reports/db"      |
      | ReactiveRxJavaDBManager | 200            | 1000           | 700            | 0.02              | "./reports/db"      |
      | ZioDBManager            | 200            | 1000           | 700            | 0.02              | "./reports/db/zio"  |
      | CatsDBManager           | 200            | 1000           | 700            | 0.02              | "./reports/db/cats" |
