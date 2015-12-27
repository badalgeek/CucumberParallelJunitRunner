package io.badal.cucumber;
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

import cucumber.api.CucumberOptions;
import io.badal.cucumber.api.ParallelCucumber;
import io.badal.cucumber.api.ParallelCucumberConfig;
import org.junit.runner.RunWith;

@RunWith(ParallelCucumber.class)
@CucumberOptions(plugin = {"html:target/cucumber-report.html"})
@ParallelCucumberConfig(noOfRetries = 2, noOfThread = 3, parentSpringContext = {"application-context.xml"},
        childSpringContext = {"child-application-context.xml"})
public class RunCukesTest {
}
