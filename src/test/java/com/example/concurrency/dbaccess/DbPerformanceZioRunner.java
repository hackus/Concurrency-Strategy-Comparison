package com.example.concurrency.dbaccess;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@Suite
@SelectClasspathResource("features/db_performance_ZIO.feature")
@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "com.example.concurrency"
)
public class DbPerformanceZioRunner {
}