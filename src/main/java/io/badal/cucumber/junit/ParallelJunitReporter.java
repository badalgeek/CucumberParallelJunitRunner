/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.badal.cucumber.junit;

import cucumber.api.PendingException;
import cucumber.runtime.CucumberException;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static cucumber.runtime.Runtime.isPending;

/**
 * Created by sbadal on 10/18/15.
 */
public class ParallelJunitReporter implements Reporter, Formatter, ResultAggregator<ParallelJunitReporter.ReporterAndFormatter> {

    private List<Consumer<ReporterAndFormatter>> events = new ArrayList<>();
    private EachTestNotifier executionUnitNotifier;
    private boolean inScenarioLifeCycle;
    private final List<Step> steps = new ArrayList<Step>();
    private final boolean strict;
    private EachTestNotifier stepNotifier;
    private boolean ignoredStep;
    private RunNotifier runNotifier;
    private ParallelExecutionUnitRunner parallelExecutionUnitRunner;
    private boolean lastRetry = false;
    private Result result;

    public ParallelJunitReporter() {
        this(false);
    }

    public ParallelJunitReporter(boolean strict) {
        this.strict = strict;
    }

    public boolean isLastRetry() {
        return lastRetry;
    }

    public void setLastRetry(boolean lastRetry) {
        this.lastRetry = lastRetry;
    }

    @Override
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {
        events.add((rf) -> {
            rf.getFormatter().syntaxError(state, event, legalEvents, uri, line);
        });
    }

    @Override
    public void uri(String uri) {
        events.add((rf) -> {
            rf.getFormatter().uri(uri);
        });
    }

    @Override
    public void feature(Feature feature) {
        events.add((rf) -> {
            rf.getFormatter().feature(feature);
        });
    }

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
        events.add((rf) -> {
            rf.getFormatter().scenarioOutline(scenarioOutline);
        });
    }

    @Override
    public void examples(Examples examples) {
        events.add((rf) -> {
            rf.getFormatter().examples(examples);
        });
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
        inScenarioLifeCycle = true;
        events.add((rf) -> {
            rf.getFormatter().startOfScenarioLifeCycle(scenario);
        });
    }

    @Override
    public void background(Background background) {
        events.add((rf) -> {
            rf.getFormatter().background(background);
        });
    }

    @Override
    public void scenario(Scenario scenario) {
        events.add((rf) -> {
            rf.getFormatter().scenario(scenario);
        });
    }

    @Override
    public void step(Step step) {
        if (inScenarioLifeCycle) {
            steps.add(step);
        }
        events.add((rf) -> {
            rf.getFormatter().step(step);
        });
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
        inScenarioLifeCycle = false;
        events.add((rf) -> {
            rf.getFormatter().endOfScenarioLifeCycle(scenario);
        });
    }

    @Override
    public void done() {
        events.add((rf) -> {
            rf.getFormatter().done();
        });
    }

    @Override
    public void close() {
        events.add((rf) -> {
            rf.getFormatter().close();
        });
    }

    @Override
    public void eof() {
        events.add((rf) -> {
            rf.getFormatter().eof();
        });
    }

    @Override
    public void before(Match match, Result result) {
        handleHook(result);
        events.add((rf) -> {
            rf.getReporter().before(match, result);
        });
    }

    @Override
    public void after(Match match, Result result) {
        handleHook(result);
        events.add((rf) -> {
            rf.getReporter().after(match, result);
        });
    }

    @Override
    public void result(Result result) {
        Throwable error = result.getError();
        if (Result.SKIPPED == result && isLastRetry()) {
            stepNotifier.fireTestIgnored();
        } else if (isPendingOrUndefined(result) && isLastRetry()) {
            addFailureOrIgnoreStep(result);
        } else {
            if (stepNotifier != null) {
                //Should only fireTestStarted if not ignored
                stepNotifier.fireTestStarted();
                if (error != null) {
                    stepNotifier.addFailure(error);
                }
                stepNotifier.fireTestFinished();
            }
            if (error != null && isLastRetry()) {
                executionUnitNotifier.addFailure(error);
            }
        }
        if (steps.isEmpty()) {
            // We have run all of our steps. Set the stepNotifier to null so that
            // if an error occurs in an After block, it's reported against the scenario
            // instead (via executionUnitNotifier).
            stepNotifier = null;
        }

        events.add((rf) -> {
            rf.getReporter().result(result);
        });
    }

    @Override
    public void match(Match match) {
        Step runnerStep = fetchAndCheckRunnerStep();
        Description description = parallelExecutionUnitRunner.describeStep(runnerStep);
        stepNotifier = new EachTestNotifier(runNotifier, description);
        events.add((rf) -> {
            rf.getReporter().match(match);
        });
    }

    @Override
    public void embedding(String mimeType, byte[] data) {
        events.add((rf) -> {
            rf.getReporter().embedding(mimeType, data);
        });
    }

    @Override
    public void write(String text) {
        events.add((rf) -> {
            rf.getReporter().write(text);
        });
    }

    private void addFailureOrIgnoreStep(Result result) {
        if (strict) {
            stepNotifier.fireTestStarted();
            addFailure(result);
            stepNotifier.fireTestFinished();
        } else {
            ignoredStep = true;
            stepNotifier.fireTestIgnored();
        }
    }

    private void addFailure(Result result) {
        Throwable error = result.getError();
        if (error == null) {
            error = new PendingException();
        }
        stepNotifier.addFailure(error);
        if(isLastRetry()) {
            executionUnitNotifier.addFailure(error);
        }
    }

    private void handleHook(Result result) {
        if (result.getStatus().equals(Result.FAILED) && isLastRetry()) {
            executionUnitNotifier.addFailure(result.getError());
        }
    }


    private Step fetchAndCheckRunnerStep() {
        Step scenarioStep = steps.remove(0);
        Step runnerStep = parallelExecutionUnitRunner.getRunnerSteps().remove(0);
        if (!scenarioStep.getName().equals(runnerStep.getName())) {
            throw new CucumberException("Expected step: \"" + scenarioStep.getName() + "\" got step: \"" + runnerStep.getName() + "\"");
        }
        return runnerStep;
    }

    private boolean isPendingOrUndefined(Result result) {
        Throwable error = result.getError();
        return Result.UNDEFINED == result || isPending(error);
    }

    public void startExecutionUnit(ParallelExecutionUnitRunner parallelExecutionUnitRunner, RunNotifier notifier) {
        this.parallelExecutionUnitRunner = parallelExecutionUnitRunner;
        this.runNotifier = notifier;
        this.stepNotifier = null;
        this.ignoredStep = false;

        executionUnitNotifier = new EachTestNotifier(notifier, parallelExecutionUnitRunner.getDescription());
        executionUnitNotifier.fireTestStarted();
    }

    public void finishExecutionUnit() {
        if (ignoredStep) {
            executionUnitNotifier.fireTestIgnored();
        }
        executionUnitNotifier.fireTestFinished();
    }

    @Override
    public void aggregate(ResultAggregator resultAggregator) {
        this.events.addAll(resultAggregator.getStream());
    }

    @Override
    public List<Consumer<ReporterAndFormatter>> getStream() {
        return events;
    }

    public static class ReporterAndFormatter {
        private Reporter reporter;
        private Formatter formatter;

        public ReporterAndFormatter(Reporter reporter, Formatter formatter) {
            this.reporter = reporter;
            this.formatter = formatter;
        }

        public Reporter getReporter() {
            return reporter;
        }

        public Formatter getFormatter() {
            return formatter;
        }

    }
}
