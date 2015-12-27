package io.badal.cucumber.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Provide the parent spring context. This is the only posssible way to maintain one parent context across
 * all test cases until glue support passing of object from junit runner to object factory.
 *
 * The only limitation it present is that we can't run junit classes in parallel, ParallelCucumber is not
 * providing such feature anyways. It only support running test which are targeted from one junit file at this
 * point of time.
 *
 * Created by badal on 12/21/15.
 */
public class SpringParentContextProvider {

    public static ConfigurableApplicationContext parentContext;
    public static int hashCode;

    public synchronized static ApplicationContext getParentSpringContext(String[] contextConfigurationFiles) {
        int requestHashCode = getArrayHash(contextConfigurationFiles);
        if (hashCode != requestHashCode) {
            if(parentContext != null) {
                parentContext.close();
            }
            parentContext = new ClassPathXmlApplicationContext(contextConfigurationFiles);
            hashCode = requestHashCode;
        }
        return parentContext;
    }

    public static int getArrayHash(String[] stringArray) {
        final int prime = 31;
        int result = 1;
        for( String s : stringArray )
        {
            result = result * prime + s.hashCode();
        }
        return result;
    }
}
