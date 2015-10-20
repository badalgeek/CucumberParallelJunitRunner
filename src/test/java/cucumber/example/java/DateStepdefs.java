
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
package cucumber.example.java;

import cucumber.api.Format;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import java.util.Date;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class DateStepdefs {
    private String result;
    private DateCalculator calculator;
    private static int i =0;

    @Given("^today is (.+)$")
    public void today_is(@Format("yyyy-MM-dd") Date date) {
        calculator = new DateCalculator(date);
    }

    /**
     * We don't need to use @Format here, since the date string in the step
     * conforms to <code>SimpleDateFormat.SHORT</code>. Cucumber has built-in support for
     * <code>SimpleDateFormat.SHORT</code>, <code>SimpleDateFormat.MEDIUM</code>,
     * <code>SimpleDateFormat.LONG</code> and <code>SimpleDateFormat.FULL</code>.
     *
     * @see <a href="http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a>
     */
    @When("^I ask if (.+) is in the past$")
    public void I_ask_if_date_is_in_the_past(Date date) {
        result = calculator.isDateInThePast(date);
    }

    @Then("^the result should be (.+)$")
    public void the_result_should_be(String expectedResult) {
        assertEquals(expectedResult, result);
    }
}
