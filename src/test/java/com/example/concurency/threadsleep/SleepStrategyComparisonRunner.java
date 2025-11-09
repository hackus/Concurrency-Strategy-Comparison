package com.example.concurency.threadsleep;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@Suite
@SelectClasspathResource("features/sleep_strategy_performance.feature")
@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "com.example.concurency"
)
public class SleepStrategyComparisonRunner {
}