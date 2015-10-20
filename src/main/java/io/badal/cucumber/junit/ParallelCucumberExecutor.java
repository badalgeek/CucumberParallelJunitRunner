
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

import cucumber.runtime.*;
import cucumber.runtime.Runtime;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberScenario;
import gherkin.formatter.Reporter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    public ParallelCucumberExecutor(ResourceLoader resourceLoader, ClassLoader classLoader,
                                    RuntimeOptions runtimeOptions) {
        this.resourceLoader = resourceLoader;
        this.classLoader = classLoader;
        this.runtimeOptions = runtimeOptions;
        this.classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
    }

    public Future<ParallelJunitReporter> executeScenario(ParallelExecutionUnitRunner parentRunner, CucumberScenario cucumberScenario,
                                RunNotifier notifier) {
        return executorService.submit(() -> {
            int retry = 0;
            ParallelJunitReporter parallelJunitReporter = new ParallelJunitReporter(runtimeOptions.isStrict());
            while(++retry <= maxRetryCount) {
                Runtime runtime = createRuntime();
                parallelJunitReporter.startExecutionUnit(parentRunner, notifier);
                cucumberScenario.run(parallelJunitReporter, parallelJunitReporter, runtime);
                if(runtime.getErrors().size() == 0){
                    break;
                }
                parentRunner.initializeChildForRetry();
                if (retry == maxRetryCount - 1) {
                   parallelJunitReporter.setLastRetry(true);
                }
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

    private Runtime createRuntime() {
        return new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
    }

    private static int getMaxRetryCount() {
        String threads = System.getProperty("Retry");
        if (threads != null) {
            return Integer.parseInt(threads);
        }
        return 3;
    }

    private static int getThreadPoolSize() {
        String threads = System.getProperty("Threads");
        if (threads != null) {
            return Integer.parseInt(threads);
        }
        return 3;
    }
}
