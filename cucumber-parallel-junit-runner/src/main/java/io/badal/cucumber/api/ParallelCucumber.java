
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

package io.badal.cucumber.api;

import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import io.badal.cucumber.CucumberConfigProvider;
import io.badal.cucumber.CucumberProperties;
import io.badal.cucumber.junit.ParallelCucumberExecutor;
import io.badal.cucumber.junit.ParallelFeatureRunner;
import io.badal.cucumber.junit.ParallelJunitReporter;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Created by badal on 10/18/15.
 */
public class ParallelCucumber extends ParentRunner<ParallelFeatureRunner> {

    private List<ParallelFeatureRunner> children = new ArrayList<ParallelFeatureRunner>();
    private List<Future<ParallelJunitReporter>> features;
    private ParallelCucumberExecutor parallelCucumberExecutor;
    private ExecutorService executorService;

    public ParallelCucumber(Class<?> testClass) throws InitializationError {
        super(testClass);
        initializeConfiguration();
        this.executorService = Executors.newCachedThreadPool();
        features = new ArrayList<>();

        ClassLoader classLoader = testClass.getClassLoader();
        ResourceLoader resourceLoader = new MultiLoader(classLoader);

        RuntimeOptions runtimeOptions = getRuntimeOptions(testClass);

        this.parallelCucumberExecutor = new ParallelCucumberExecutor(resourceLoader,
                classLoader, runtimeOptions);

        List<CucumberFeature> cucumberFeatures = runtimeOptions.cucumberFeatures(resourceLoader);
        addChildren(cucumberFeatures, parallelCucumberExecutor);
    }

    private void initializeConfiguration() {
        ParallelCucumberConfig annotation = this.getTestClass().getAnnotation(ParallelCucumberConfig.class);
        if (annotation != null) {
            CucumberConfigProvider cucumberConfigProvider = CucumberConfigProvider.getInstance();
            cucumberConfigProvider.setProperty(CucumberProperties.RetryCount.name(), annotation.noOfRetries());
            cucumberConfigProvider.setProperty(CucumberProperties.ThreadCount.name(), annotation.noOfThread());
            cucumberConfigProvider.setProperty(CucumberProperties.SpringChildContext.name(), annotation.parentSpringContext());
            cucumberConfigProvider.setProperty(CucumberProperties.SpringParentContext.name(), annotation.childSpringContext());
        }
    }

    private RuntimeOptions getRuntimeOptions(Class<?> testClass) {
        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(testClass);
        return runtimeOptionsFactory.create();
    }

    private void addChildren(List<CucumberFeature> cucumberFeatures, ParallelCucumberExecutor executor) throws InitializationError {
        for (CucumberFeature cucumberFeature : cucumberFeatures) {
            children.add(new ParallelFeatureRunner(cucumberFeature, executor, executorService));
        }
    }

    @Override
    protected List<ParallelFeatureRunner> getChildren() {
        return children;
    }

    @Override
    protected Description describeChild(ParallelFeatureRunner parallelFeatureRunner) {
        return parallelFeatureRunner.getDescription();
    }

    @Override
    protected void runChild(ParallelFeatureRunner parallelFeatureRunner, RunNotifier runNotifier) {
        features.add(parallelFeatureRunner.runAsync(runNotifier));
    }

    @Override
    public void run(RunNotifier notifier) {
        super.run(notifier);
        ParallelJunitReporter.ReporterAndFormatter reporterAndFormatter = parallelCucumberExecutor.getReporterAndFormatter();
        for (Future<ParallelJunitReporter> feature : features) {
            try {
                ParallelJunitReporter parallelJunitReporter = feature.get();
                for (Consumer<ParallelJunitReporter.ReporterAndFormatter> consumer : parallelJunitReporter.getStream()) {
                    consumer.accept(reporterAndFormatter);
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        reporterAndFormatter.getFormatter().done();
        reporterAndFormatter.getFormatter().close();

        try {
            this.parallelCucumberExecutor.close();
            this.executorService.shutdown();
            if (!this.executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.println("ParallelCucumber: Couldn't shut down gracefully");
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
