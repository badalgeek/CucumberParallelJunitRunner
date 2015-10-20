
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class Assertions {
    public static void assertNoCucumberAnnotatedMethods(Class clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().getName().startsWith("cucumber")) {
                    throw new CucumberException(
                            "\n\n" +
                                    "Classes annotated with @RunWith(Cucumber.class) must not define any\n" +
                                    "Step Definition or Hook methods. Their sole purpose is to serve as\n" +
                                    "an entry point for JUnit. Step Definitions and Hooks should be defined\n" +
                                    "in their own classes. This allows them to be reused across features.\n" +
                                    "Offending class: " + clazz + "\n"
                    );
                }
            }
        }
    }
}
