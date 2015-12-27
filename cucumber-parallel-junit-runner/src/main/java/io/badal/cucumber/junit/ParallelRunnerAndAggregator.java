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

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by badal on 10/18/15.
 */
public abstract class ParallelRunnerAndAggregator<T extends ParallelRunnerAndAggregator, U extends ResultAggregator>
        extends Runner {

    private U resultAggregator;
    private ExecutorService executorService;

    protected ParallelRunnerAndAggregator(ExecutorService executorService) throws InitializationError {
        super();
        this.executorService = executorService;
        resultAggregator = newAggregator();
    }

    protected abstract U newAggregator();

    protected ExecutorService getExecutorService(){
        return this.executorService;
    }

    public void run(RunNotifier notifier) {
        throw new UnsupportedOperationException();
    }

    public Future<U> runAsync(final RunNotifier notifier) {
        return executorService.submit(() -> {
            List<Future<U>> futures = new ArrayList<>();
            for (T t : getChildren()) {
                futures.add(runChildAsync(notifier, t));
            }
            for (Future<U> task : futures) {
                resultAggregator.aggregate(task.get());
            }
            return resultAggregator;
        });
    }

    protected Description describeChild(ParallelRunnerAndAggregator child) {
        return child.getDescription();
    }

    public abstract Description getDescription();

    protected Future<U> runChildAsync(RunNotifier notifier, T t) {
        return t.runAsync(notifier);
    }

    protected abstract List<T> getChildren();

    protected U getAggregator(){
        return this.resultAggregator;
    };
}
