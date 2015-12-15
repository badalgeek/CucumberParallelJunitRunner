
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

import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberScenario;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Tag;
import org.junit.runner.notification.RunNotifier;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by sbadal on 10/18/15.
 */
public class ParallelCucumberExecutor {

    private ResourceLoader resourceLoader;
    private ClassLoader classLoader;
    private RuntimeOptions runtimeOptions;
    private ClassFinder classFinder;
    private ExecutorService executorService = Executors.newFixedThreadPool(getThreadPoolSize());
    private int maxRetryCount = getMaxRetryCount();
    private final Object mutex;

    public ParallelCucumberExecutor(ResourceLoader resourceLoader, ClassLoader classLoader,
                                    RuntimeOptions runtimeOptions) {
        this.resourceLoader = resourceLoader;
        this.classLoader = classLoader;
        this.runtimeOptions = runtimeOptions;
        this.classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        this.mutex = new Object();
    }

    public Future<ParallelJunitReporter> executeScenario(ParallelExecutionUnitRunner parentRunner, CucumberScenario cucumberScenario,
                                                         RunNotifier notifier) {
        return executorService.submit(() -> {
            int retry = 0;
            ParallelJunitReporter parallelJunitReporter = new ParallelJunitReporter(runtimeOptions.isStrict());
            while (++retry <= maxRetryCount) {
                if (retry == maxRetryCount) {
                    parallelJunitReporter.setLastRetry(true);
                }
                Runtime runtime = createRuntime();
                parallelJunitReporter.startExecutionUnit(parentRunner, notifier);
                cucumberScenario.run(parallelJunitReporter, parallelJunitReporter, runtime);
                if (runtime.getErrors().size() == 0) {
                    break;
                }
                parentRunner.initializeChildForRetry();
                System.out.println("Retrying " + cucumberScenario.getVisualName());
            }
            parallelJunitReporter.finishExecutionUnit();
            return parallelJunitReporter;
        });
    }

    public ParallelJunitReporter.ReporterAndFormatter getReporterAndFormatter() {
        return new ParallelJunitReporter.ReporterAndFormatter(runtimeOptions.reporter(classLoader),
                runtimeOptions.formatter(classLoader));
    }

    public void close() throws InterruptedException {
        this.executorService.shutdown();
        if (!this.executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            System.out.println("ParallelCucumberExecutor: Couldn't shut down gracefully");;
        }
    }

    private synchronized Runtime createRuntime() {
        return new ThreadSafeEnvironment(resourceLoader, classFinder, classLoader, runtimeOptions, mutex);
    }

    private static int getMaxRetryCount() {
        String threads = System.getProperty("Retry");
        if (threads != null) {
            return Integer.parseInt(threads);
        }
        return 1;
    }

    private static int getThreadPoolSize() {
        String threads = System.getProperty("Threads");
        if (threads != null) {
            return Integer.parseInt(threads);
        }
        return 1;
    }

    private static class ThreadSafeEnvironment extends Runtime {

        private Object mutex;

        public ThreadSafeEnvironment(ResourceLoader resourceLoader, ClassFinder classFinder, ClassLoader classLoader, RuntimeOptions runtimeOptions, Object mutex) {
            super(resourceLoader, classFinder, classLoader, runtimeOptions);
            this.mutex = mutex;
        }

        public void buildBackendWorlds(Reporter reporter, Set<Tag> tags, Scenario gherkinScenario) {
            synchronized (mutex) {
                super.buildBackendWorlds(reporter, tags, gherkinScenario);
            }
        }

        public void disposeBackendWorlds(String scenarioDesignation) {
            synchronized (mutex) {
                super.disposeBackendWorlds(scenarioDesignation);
            }
        }
    }
}

