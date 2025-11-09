@dbtest
Feature: Database performance testing

  Scenario Outline: Run performance test using <strategy> strategy
    Given the H2 database <strategy> is initialized
    When I run performance tests with 2000 concurrent users and <strategy> strategy
    Then the average insert response time should be under <insert_latency> ms and percentage of failed less than <failed_percentage> percent
    And the average update response time should be under <update_latency> ms and percentage of failed less than <failed_percentage> percent
    And the average select response time should be under <select_latency> ms and percentage of failed less than <failed_percentage> percent

    Examples:
      | strategy                | insert_latency | update_latency | select_latency | failed_percentage |
      | VirtualThreads          | 200            | 1000           | 700            | 0.02              |
      | CompletableFuture       | 200            | 1000           | 700            | 0.02              |
      | ReactiveRxJavaDBManager | 200            | 1000           | 700            | 0.02              |
