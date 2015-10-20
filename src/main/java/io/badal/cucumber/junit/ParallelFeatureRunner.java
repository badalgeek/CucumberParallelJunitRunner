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

import cucumber.runtime.CucumberException;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberScenario;
import cucumber.runtime.model.CucumberScenarioOutline;
import cucumber.runtime.model.CucumberTagStatement;
import gherkin.formatter.model.Feature;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by sbadal on 10/18/15.
 */
public class ParallelFeatureRunner extends ParallelRunnerAndAggregator<ParallelRunnerAndAggregator, ParallelJunitReporter>  {

    private Description description;
    private CucumberFeature cucumberFeature;
    private final List<ParallelRunnerAndAggregator> children = new ArrayList<>();

    public ParallelFeatureRunner(CucumberFeature cucumberFeature, ParallelCucumberExecutor executor) throws InitializationError {
        super();
        this.cucumberFeature = cucumberFeature;
        addChildren(executor);
    }

    public String getName() {
        Feature feature = cucumberFeature.getGherkinFeature();
        return feature.getKeyword() + ": " + feature.getName();
    }

    @Override
    public Description getDescription() {
        if (description == null) {
            description = Description.createSuiteDescription(getName(), cucumberFeature.getGherkinFeature());
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

    private void addChildren(ParallelCucumberExecutor executor) {
        for (CucumberTagStatement cucumberTagStatement : cucumberFeature.getFeatureElements()) {
            try {
                ParallelRunnerAndAggregator featureElementRunner;
                if (cucumberTagStatement instanceof CucumberScenario) {
                    featureElementRunner = new ParallelExecutionUnitRunner((CucumberScenario) cucumberTagStatement,
                            executor);
                } else {
                    featureElementRunner = new ParallelScenarioOutlineRunner(
                            (CucumberScenarioOutline) cucumberTagStatement, executor);
                }
                children.add(featureElementRunner);
            } catch (InitializationError e) {
                throw new CucumberException("Failed to create scenario runner", e);
            }
        }
    }

    public Future<ParallelJunitReporter> runAsync(final RunNotifier notifier) {
        getAggregator().uri(cucumberFeature.getPath());
        getAggregator().feature(cucumberFeature.getGherkinFeature());
        Future<ParallelJunitReporter> future = super.runAsync(notifier);
        return future;
    }
}
