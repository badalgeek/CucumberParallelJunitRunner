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

import cucumber.api.Transformer;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.deps.com.thoughtworks.xstream.annotations.XStreamConverter;
import org.junit.Assert;

import java.util.List;

public class ShoppingStepdefs {
    private RpnCalculator calc = new RpnCalculator();

    @Given("^the following groceries:$")
    public void the_following_groceries(List<Grocery> groceries) {
        for (Grocery grocery : groceries) {
            calc.push(grocery.price.value);
            calc.push("+");
        }
    }

    @When("^I pay (\\d+)$")
    public void i_pay(int amount) {
        calc.push(amount);
        calc.push("-");
    }

    @Then("^my change should be (\\d+)$")
    public void my_change_should_be_(int change) {
        Assert.assertEquals(-calc.value().intValue(), change);
    }

    public static class Grocery {
        public String name;
        @XStreamConverter(Price.Converter.class)
        public Price price;

        public Grocery() {
            super();
        }
    }

    public static class Price {
        public int value;

        public Price(int value) {
            this.value = value;
        }

        public static class Converter extends Transformer<Price> {
            @Override
            public Price transform(String value) {
                return new Price(Integer.parseInt(value));
            }
        }
    }
}
