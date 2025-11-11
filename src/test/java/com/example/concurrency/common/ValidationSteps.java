package com.example.concurrency.common;

import com.example.concurrency.pdfreader.PdfReaderSteps;
import com.example.concurrency.threadsleep.SleepStrategyComparisonSteps;
import io.cucumber.java.en.Then;

import static org.junit.jupiter.api.Assertions.fail;

public class ValidationSteps {

    @Then("the run should complete within {int} ms")
    public void validateDurationWithin(int expectedMaxMs) {
        // Try to pull context from whichever parent class was used in this scenario
        StepContext ctx = getActiveContext();

        if (ctx == null || ctx.demoName == null) {
            fail("❌ No active StepContext found — make sure you executed a run step before validation.");
        }

        System.out.printf("Validating %s duration: %d ms (limit %d ms)%n",
            ctx.demoName, ctx.durationMs, expectedMaxMs);

        if (ctx.durationMs > expectedMaxMs) {
            fail(String.format(
                "%s took %d ms, exceeding limit of %d ms",
                ctx.demoName, ctx.durationMs, expectedMaxMs
            ));
        }
    }

    private StepContext getActiveContext() {
        // Prefer whichever context was updated in the current scenario
        StepContext pdfCtx = PdfReaderSteps.context.get();
        StepContext sleepCtx = SleepStrategyComparisonSteps.context.get();

        // Whichever has demoName set is the one that ran
        if (pdfCtx.demoName != null) return pdfCtx;
        if (sleepCtx.demoName != null) return sleepCtx;
        return null;
    }
}
