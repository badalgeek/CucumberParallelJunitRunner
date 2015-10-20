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
import cucumber.runtime.model.CucumberScenarioOutline;
import org.junit.runner.Description;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sbadal on 10/18/15.
 */
public class ParallelScenarioOutlineRunner  extends ParallelRunnerAndAggregator<ParallelRunnerAndAggregator, ParallelJunitReporter> {

    private CucumberScenarioOutline cucumberScenarioOutline;
    private Description description;
    private List<ParallelRunnerAndAggregator> children;

    public ParallelScenarioOutlineRunner(CucumberScenarioOutline cucumberTagStatement, ParallelCucumberExecutor executor) throws InitializationError {
        super();
        this.cucumberScenarioOutline = cucumberTagStatement;
        this.children = createChildren(cucumberScenarioOutline, executor);
    }

    private List<ParallelRunnerAndAggregator> createChildren(CucumberScenarioOutline cucumberScenarioOutline, ParallelCucumberExecutor executor) throws InitializationError {
        List<ParallelRunnerAndAggregator> runners = new ArrayList<>();
        for (CucumberExamples cucumberExamples : cucumberScenarioOutline.getCucumberExamplesList()) {
            runners.add(new ParallelExamplesRunner(cucumberExamples, executor));
        }
        return runners;
    }

    public String getName() {
        return cucumberScenarioOutline.getVisualName();
    }

    @Override
    public Description getDescription() {
        if (description == null) {
            description = Description.createSuiteDescription(getName(), cucumberScenarioOutline.getGherkinModel());
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
