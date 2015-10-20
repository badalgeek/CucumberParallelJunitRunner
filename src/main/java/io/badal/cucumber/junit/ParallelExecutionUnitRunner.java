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

import cucumber.runtime.model.CucumberScenario;
import gherkin.formatter.model.Step;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by sbadal on 10/18/15.
 */
public class ParallelExecutionUnitRunner
        extends ParallelRunnerAndAggregator<ParallelRunnerAndAggregator, ParallelJunitReporter>  {

    private final ParallelCucumberExecutor executor;
    private CucumberScenario cucumberScenario;
    private Description description;
    private List<Step> runnerSteps = new ArrayList<>();
    private final Map<Step, Description> stepDescriptions = new HashMap<>();

    public ParallelExecutionUnitRunner(CucumberScenario cucumberScenario, ParallelCucumberExecutor executor) throws InitializationError {
        super();
        this.cucumberScenario = cucumberScenario;
        this.executor = executor;
        initializeDescription();
    }

    @Override
    protected List<ParallelRunnerAndAggregator> getChildren() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Description getDescription() {
        if (description == null) {
            initializeDescription();
        }
        return description;
    }

    public List<Step> getRunnerSteps() {
        return runnerSteps;
    }

    private String getName() {
        return cucumberScenario.getVisualName();
    }

    @Override
    protected ParallelJunitReporter newAggregator() {
        return new ParallelJunitReporter();
    }

    @Override
    public Future<ParallelJunitReporter> runAsync(final RunNotifier notifier) {
        return executor.executeScenario(this, cucumberScenario, notifier);
    }

    public void initializeChildForRetry(){
        if (cucumberScenario.getCucumberBackground() != null) {
            for (Step backgroundStep : cucumberScenario.getCucumberBackground().getSteps()) {
                // We need to make a copy of that step, so we have a unique one per scenario
                Step copy = new Step(
                        backgroundStep.getComments(),
                        backgroundStep.getKeyword(),
                        backgroundStep.getName(),
                        backgroundStep.getLine(),
                        backgroundStep.getRows(),
                        backgroundStep.getDocString()
                );
                runnerSteps.add(copy);
            }
        }

        for (Step step : cucumberScenario.getSteps()) {
            runnerSteps.add(step);
        }
    }

    public Description describeStep(Step step) {
        Description description = stepDescriptions.get(step);
        if (description == null) {
            description = Description.createTestDescription(getName(), step.getKeyword() + step.getName(), step);
            stepDescriptions.put(step, description);
        }
        return description;
    }

    private void initializeDescription() {
        description = Description.createSuiteDescription(getName(), cucumberScenario.getGherkinModel());

        if (cucumberScenario.getCucumberBackground() != null) {
            for (Step backgroundStep : cucumberScenario.getCucumberBackground().getSteps()) {
                // We need to make a copy of that step, so we have a unique one per scenario
                Step copy = new Step(
                        backgroundStep.getComments(),
                        backgroundStep.getKeyword(),
                        backgroundStep.getName(),
                        backgroundStep.getLine(),
                        backgroundStep.getRows(),
                        backgroundStep.getDocString()
                );
                description.addChild(describeStep(copy));
                runnerSteps.add(copy);
            }
        }

        for (Step step : cucumberScenario.getSteps()) {
            description.addChild(describeStep(step));
            runnerSteps.add(step);
        }
    }
}
