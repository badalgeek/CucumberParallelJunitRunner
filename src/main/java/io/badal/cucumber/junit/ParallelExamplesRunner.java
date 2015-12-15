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

import cucumber.runtime.model.CucumberExamples;
import cucumber.runtime.model.CucumberScenario;
import org.junit.runner.Description;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by badal on 10/18/15.
 */
public class ParallelExamplesRunner  extends ParallelRunnerAndAggregator<ParallelRunnerAndAggregator, ParallelJunitReporter>{

    private final CucumberExamples cucumberExamples;
    private Description description;
    private final List<ParallelRunnerAndAggregator> children;

    public ParallelExamplesRunner(CucumberExamples cucumberExamples, ParallelCucumberExecutor executor, ExecutorService executorService)
            throws InitializationError {
        super(executorService);
        this.cucumberExamples = cucumberExamples;
        this.children = this.createChildren(cucumberExamples, executor);
    }

    private List<ParallelRunnerAndAggregator> createChildren(CucumberExamples cucumberExamples, ParallelCucumberExecutor executor) {
        List<ParallelRunnerAndAggregator> runners = new ArrayList<>();
        List<CucumberScenario> exampleScenarios = cucumberExamples.createExampleScenarios();
        for (CucumberScenario scenario : exampleScenarios) {
            try {
                ParallelExecutionUnitRunner exampleScenarioRunner = new ParallelExecutionUnitRunner(scenario, executor, getExecutorService());
                runners.add(exampleScenarioRunner);
            } catch (InitializationError initializationError) {
                initializationError.printStackTrace();
            }
        }
        return runners;
    }

    protected String getName() {
        return cucumberExamples.getExamples().getKeyword() + ": " + cucumberExamples.getExamples().getName();
    }

    @Override
    public Description getDescription() {
        if (description == null) {
            description = Description.createSuiteDescription(getName(), cucumberExamples.getExamples());
            for (ParallelRunnerAndAggregator child : getChildren()) {
                description.addChild(describeChild(child));
            }
        }
        return description;
    }

    @Override
    protected List<ParallelRunnerAndAggregator> getChildren() {
        return children;
    }

    @Override
    protected ParallelJunitReporter newAggregator() {
        return new ParallelJunitReporter();
    }
}
