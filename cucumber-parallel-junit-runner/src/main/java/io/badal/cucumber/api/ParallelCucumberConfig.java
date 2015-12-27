package io.badal.cucumber.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by sbadal on 12/18/15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ParallelCucumberConfig {

    int noOfThread() default 1;
    int noOfRetries() default 1;
    String[] parentSpringContext() default "";
    String[] childSpringContext() default "";
}